package bhoon.sugang_helper.crawling.application;

import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.domain.CourseRepository;
import bhoon.sugang_helper.course.domain.CourseSchedule;
import bhoon.sugang_helper.course.domain.CourseSeatHistory;
import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.course.domain.SeatOpenedEvent;
import bhoon.sugang_helper.course.infra.CourseSeatHistoryJpaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseSynchronizationService {

    private static final int CHUNK_SIZE = 100;

    private final CourseRepository courseRepository;
    private final CourseSeatHistoryJpaRepository courseSeatHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
    private final CrawlerRunStateService crawlerRunStateService;

    @Transactional
    public void synchronize(List<ParsedCourseDto> parsedCourses) {
        Set<String> seenStdtrNos = new HashSet<>();
        for (int offset = 0; offset < parsedCourses.size(); offset += CHUNK_SIZE) {
            int end = Math.min(offset + CHUNK_SIZE, parsedCourses.size());
            synchronizeChunk(parsedCourses.subList(offset, end), seenStdtrNos);
        }
        crawlerRunStateService.markSuccess();
    }

    private void synchronizeChunk(List<ParsedCourseDto> chunk, Set<String> seenStdtrNos) {
        long startedAt = System.nanoTime();
        List<CourseSeatHistory> seatHistories = new ArrayList<>();
        List<Course> newCourses = new ArrayList<>();
        List<Course> crawledCourses = chunk.stream().map(this::mapToEntity).toList();
        recordDuplicateStdtrNos(crawledCourses, seenStdtrNos);
        Map<String, Course> existingCourses = findExistingCourses(crawledCourses);
        for (Course crawledCourse : crawledCourses) {
            CourseSeatHistory seatHistory = processCourse(crawledCourse, existingCourses, newCourses);
            if (seatHistory != null) {
                seatHistories.add(seatHistory);
            }
        }
        if (!newCourses.isEmpty()) {
            courseRepository.saveAll(newCourses);
        }
        if (!seatHistories.isEmpty()) {
            courseSeatHistoryRepository.saveAll(seatHistories);
        }
        recordChunkWriteMetric(chunk.size(), startedAt);
    }

    private Map<String, Course> findExistingCourses(List<Course> crawledCourses) {
        if (crawledCourses.isEmpty()) {
            return new HashMap<>();
        }
        List<String> courseKeys = crawledCourses.stream().map(Course::getCourseKey).distinct().toList();
        return courseRepository.findByCourseKeyIn(courseKeys).stream()
                .collect(Collectors.toMap(Course::getCourseKey, Function.identity()));
    }

    private CourseSeatHistory processCourse(Course crawledCourse, Map<String, Course> existingCourses,
                                             List<Course> newCourses) {
        Course existingCourse = existingCourses.get(crawledCourse.getCourseKey());
        if (existingCourse != null) {
            return updateExistingCourse(existingCourse, crawledCourse);
        }
        newCourses.add(crawledCourse);
        existingCourses.put(crawledCourse.getCourseKey(), crawledCourse);
        return toSeatHistory(crawledCourse);
    }

    private Course mapToEntity(ParsedCourseDto dto) {
        Course course = Course.builder()
                .courseKey(canonical(dto.courseKey()))
                .subjectCode(canonical(dto.subjectCode()))
                .stdtrNo(canonical(dto.stdtrNo()))
                .classNumber(canonical(dto.classNumber()))
                .name(canonical(dto.name()))
                .professor(canonical(dto.professor()))
                .capacity(dto.capacity())
                .current(dto.current())
                .targetGrade(dto.targetGrade())
                .academicYear(canonical(dto.academicYear()))
                .semester(canonical(dto.semester()))
                .classification(dto.classification())
                .department(canonical(dto.department()))
                .gradingMethod(dto.gradingMethod())
                .lectureLanguage(dto.lectureLanguage())
                .classTime(canonical(dto.classTime()))
                .credits(canonical(dto.credits()))
                .disclosure(dto.disclosure())
                .disclosureReason(canonical(dto.disclosureReason()))
                .lectureHours(dto.lectureHours())
                .generalCategory(canonical(dto.generalCategory()))
                .generalDetail(canonical(dto.generalDetail()))
                .accreditation(dto.accreditation())
                .status(dto.status())
                .classroom(canonical(dto.classroom()))
                .hasSyllabus(dto.hasSyllabus())
                .generalCategoryByYear(canonical(dto.generalCategoryByYear()))
                .courseDirection(canonical(dto.courseDirection()))
                .classDuration(canonical(dto.classDuration()))
                .build();

        if (dto.schedules() != null) {
            dto.schedules().stream()
                    .sorted(Comparator
                            .comparing((ParsedCourseDto.ScheduleDto schedule) -> schedule.dayOfWeek().ordinal())
                            .thenComparing(ParsedCourseDto.ScheduleDto::startTime)
                            .thenComparing(ParsedCourseDto.ScheduleDto::endTime))
                    .forEach(schedule -> course.addSchedule(new CourseSchedule(
                            schedule.dayOfWeek(), schedule.startTime(), schedule.endTime())));
        }
        return course;
    }

    private String canonical(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private CourseSeatHistory updateExistingCourse(Course existingCourse, Course crawledCourse) {
        boolean wasFull = existingCourse.getAvailable() <= 0;
        boolean seatsChanged = !existingCourse.getCapacity().equals(crawledCourse.getCapacity())
                || !existingCourse.getCurrent().equals(crawledCourse.getCurrent());

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
        log.info("[Crawler] Seat opening detected. courseName={}, available={}", course.getName(),
                course.getAvailable());
        eventPublisher.publishEvent(new SeatOpenedEvent(
                course.getCourseKey(), course.getName(), course.getProfessor(), 0, course.getAvailable()));
    }

    private void recordChunkWriteMetric(int chunkSize, long startedAt) {
        if (meterRegistry == null) {
            return;
        }
        Timer timer = meterRegistry.timer("crawler.course.chunk.write");
        if (timer != null) {
            timer.record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
        }
        io.micrometer.core.instrument.Counter counter = meterRegistry.counter("crawler.course.chunk.items");
        if (counter != null) {
            counter.increment(chunkSize);
        }
    }

    private void recordDuplicateStdtrNos(List<Course> crawledCourses, Set<String> seenStdtrNos) {
        long duplicateRows = countDuplicateStdtrNos(crawledCourses, seenStdtrNos);
        if (duplicateRows != 0 && meterRegistry != null) {
            io.micrometer.core.instrument.Counter counter =
                    meterRegistry.counter("crawler.course.stdtr_no.duplicates");
            if (counter != null) {
                counter.increment(duplicateRows);
            }
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
}
