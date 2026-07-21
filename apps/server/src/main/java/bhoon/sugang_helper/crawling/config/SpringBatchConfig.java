package bhoon.sugang_helper.crawling.config;

import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.domain.CourseRepository;
import bhoon.sugang_helper.course.domain.CourseSchedule;
import bhoon.sugang_helper.course.domain.CourseSeatHistory;
import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.course.domain.SeatOpenedEvent;
import bhoon.sugang_helper.crawling.application.JbnuCourseParser;
import bhoon.sugang_helper.crawling.application.JbnuCourseItemReader;
import bhoon.sugang_helper.crawling.infra.JbnuCourseApiClient;
import bhoon.sugang_helper.course.infra.CourseSeatHistoryJpaRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SpringBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JbnuCourseApiClient apiClient;
    private final JbnuCourseParser courseParser;
    private final CourseRepository courseRepository;
    private final CourseSeatHistoryJpaRepository courseSeatHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    @Bean
    public Job crawlJob(Step crawlStep) {
        return new JobBuilder("crawlJob", jobRepository)
                .start(crawlStep)
                .build();
    }

    @Bean
    public Step crawlStep(ItemReader<ParsedCourseDto> crawlReader, ItemWriter<ParsedCourseDto> crawlWriter) {
        return new StepBuilder("crawlStep", jobRepository)
                .<ParsedCourseDto, ParsedCourseDto>chunk(100, transactionManager)
                .reader(crawlReader)
                .writer(crawlWriter)
                .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<ParsedCourseDto> crawlReader(
            @Value("#{jobParameters['year']}") String year,
            @Value("#{jobParameters['semester']}") String semester) {
        log.info("[SpringBatchConfig] Initializing crawlReader for year={}, semester={}", year, semester);

        return new JbnuCourseItemReader(apiClient, courseParser, year, semester);
    }

    @Bean
    @StepScope
    public ItemWriter<ParsedCourseDto> crawlWriter() {
        Set<String> seenStdtrNos = new HashSet<>();
        return chunk -> {
            log.info("[SpringBatchConfig] Writing chunk of {} courses.", chunk.size());
            long startedAt = System.nanoTime();
            List<CourseSeatHistory> seatHistories = new ArrayList<>();
            List<Course> crawledCourses = chunk.getItems().stream().map(this::mapToEntity).toList();
            recordDuplicateStdtrNos(crawledCourses, seenStdtrNos);
            Map<String, Course> existingCourses = findExistingCourses(crawledCourses);
            for (Course crawledCourse : crawledCourses) {
                CourseSeatHistory seatHistory = processCourse(crawledCourse, existingCourses);
                if (seatHistory != null) {
                    seatHistories.add(seatHistory);
                }
            }
            if (!seatHistories.isEmpty()) {
                courseSeatHistoryRepository.saveAll(seatHistories);
            }
            recordChunkWriteMetric(chunk.size(), startedAt);
        };
    }

    private Map<String, Course> findExistingCourses(List<Course> crawledCourses) {
        if (crawledCourses.isEmpty()) {
            return new HashMap<>();
        }
        List<String> courseKeys = crawledCourses.stream().map(Course::getCourseKey).distinct().toList();
        return courseRepository.findByCourseKeyIn(courseKeys).stream()
                .collect(Collectors.toMap(Course::getCourseKey, Function.identity()));
    }

    private CourseSeatHistory processCourse(Course crawledCourse, Map<String, Course> existingCourses) {
        Course existingCourse = existingCourses.get(crawledCourse.getCourseKey());
        if (existingCourse != null) {
            return updateExistingCourse(existingCourse, crawledCourse);
        }
        CourseSeatHistory history = createNewCourse(crawledCourse);
        existingCourses.put(crawledCourse.getCourseKey(), crawledCourse);
        return history;
    }

    private Course mapToEntity(ParsedCourseDto dto) {
        Course course = Course.builder()
                .courseKey(dto.courseKey())
                .subjectCode(dto.subjectCode())
                .stdtrNo(dto.stdtrNo())
                .classNumber(dto.classNumber())
                .name(dto.name())
                .professor(dto.professor())
                .capacity(dto.capacity())
                .current(dto.current())
                .targetGrade(dto.targetGrade())
                .academicYear(dto.academicYear())
                .semester(dto.semester())
                .classification(dto.classification())
                .department(dto.department())
                .gradingMethod(dto.gradingMethod())
                .lectureLanguage(dto.lectureLanguage())
                .classTime(dto.classTime())
                .credits(dto.credits())
                .disclosure(dto.disclosure())
                .disclosureReason(dto.disclosureReason())
                .lectureHours(dto.lectureHours())
                .generalCategory(dto.generalCategory())
                .generalDetail(dto.generalDetail())
                .accreditation(dto.accreditation())
                .status(dto.status())
                .classroom(dto.classroom())
                .hasSyllabus(dto.hasSyllabus())
                .generalCategoryByYear(dto.generalCategoryByYear())
                .courseDirection(dto.courseDirection())
                .classDuration(dto.classDuration())
                .build();

        if (dto.schedules() != null) {
            dto.schedules()
                    .forEach(s -> course.addSchedule(new CourseSchedule(s.dayOfWeek(), s.startTime(), s.endTime())));
        }
        return course;
    }

    private CourseSeatHistory createNewCourse(Course course) {
        courseRepository.save(course);
        return toSeatHistory(course);
    }

    private void recordChunkWriteMetric(int chunkSize, long startedAt) {
        if (meterRegistry == null) {
            return;
        }
        Timer timer = meterRegistry.timer("crawler.course.chunk.write");
        if (timer != null) {
            timer.record(System.nanoTime() - startedAt, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
        io.micrometer.core.instrument.Counter.builder("crawler.course.chunk.items")
                .register(meterRegistry)
                .increment(chunkSize);
    }

    private void recordDuplicateStdtrNos(List<Course> crawledCourses, Set<String> seenStdtrNos) {
        long duplicateRows = countDuplicateStdtrNos(crawledCourses, seenStdtrNos);
        if (duplicateRows == 0) {
            return;
        }
        log.warn("[SpringBatchConfig] Duplicate stdtrNo values detected. duplicateRows={}", duplicateRows);
        if (meterRegistry != null) {
            meterRegistry.counter("crawler.course.stdtr_no.duplicates").increment(duplicateRows);
        }
    }

    long countDuplicateStdtrNos(List<Course> crawledCourses, Set<String> seenStdtrNos) {
        long duplicateRows = 0;
        for (Course course : crawledCourses) {
            String stdtrNo = course.getStdtrNo();
            if (stdtrNo != null && !seenStdtrNos.add(stdtrNo)) {
                duplicateRows++;
            }
        }
        return duplicateRows;
    }

    private CourseSeatHistory updateExistingCourse(Course existingCourse, Course crawledCourse) {
        boolean wasFull = existingCourse.getAvailable() <= 0;
        boolean seatsChanged = !existingCourse.getCapacity().equals(crawledCourse.getCapacity()) ||
                !existingCourse.getCurrent().equals(crawledCourse.getCurrent());

        existingCourse.updateMetadata(crawledCourse);

        if (wasFull && existingCourse.getAvailable() > 0) {
            publishSeatOpenedEvent(existingCourse);
        }

        return seatsChanged ? toSeatHistory(existingCourse) : null;
    }

    private CourseSeatHistory toSeatHistory(Course course) {
        return CourseSeatHistory.builder()
                .courseKey(course.getCourseKey())
                .capacity(course.getCapacity())
                .current(course.getCurrent())
                .build();
    }

    private void publishSeatOpenedEvent(Course course) {
        log.info("[SpringBatchConfig] Seat opening detected. courseName={}, available={}", course.getName(),
                course.getAvailable());
        eventPublisher.publishEvent(new SeatOpenedEvent(
                course.getCourseKey(),
                course.getName(),
                course.getProfessor(),
                0,
                course.getAvailable()));
    }
}
