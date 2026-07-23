package bhoon.sugang_helper.crawling.config;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.domain.CourseDayOfWeek;
import bhoon.sugang_helper.course.domain.CourseSchedule;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CourseBatchOptimizationTest {

    private static final String SAMPLE_COURSE_KEY = "COMP101:01";

    @Test
    @DisplayName("동일한 필드를 가진 강의 비교 시 hasMetadataOrScheduleChanged는 false를 반환하고 updateMetadata는 변경을 차단한다")
    void identicalCourse_ReturnsFalseAndSkipsUpdate() {
        // given
        Course original = createSampleCourse(SAMPLE_COURSE_KEY, 50, 20);
        LocalDateTime originalLastCrawled = original.getLastCrawledAt();

        Course crawledIdentical = createSampleCourse(SAMPLE_COURSE_KEY, 50, 20);

        // when
        boolean changed = original.hasMetadataOrScheduleChanged(crawledIdentical);
        boolean updated = original.updateMetadata(crawledIdentical);

        // then
        assertThat(changed).isFalse();
        assertThat(updated).isFalse();
        assertThat(original.getLastCrawledAt()).isEqualTo(originalLastCrawled);
    }

    @Test
    @DisplayName("수강인원이 변경된 강의 비교 시 updateMetadata는 true를 반환하고 lastCrawledAt을 갱신한다")
    void modifiedCourse_ReturnsTrueAndUpdatesMetadata() {
        // given
        Course original = createSampleCourse(SAMPLE_COURSE_KEY, 50, 20);
        Course crawledModified = createSampleCourse(SAMPLE_COURSE_KEY, 50, 25);

        // when
        boolean changed = original.hasMetadataOrScheduleChanged(crawledModified);
        boolean updated = original.updateMetadata(crawledModified);

        // then
        assertThat(changed).isTrue();
        assertThat(updated).isTrue();
        assertThat(original.getCurrent()).isEqualTo(25);
    }

    @Test
    @DisplayName("시간표가 달라진 경우 updateMetadata가 true를 반환한다")
    void modifiedSchedule_ReturnsTrueAndUpdatesSchedules() {
        // given
        Course original = createSampleCourse(SAMPLE_COURSE_KEY, 50, 20);

        Course crawledWithNewSchedule = createSampleCourse(SAMPLE_COURSE_KEY, 50, 20);
        crawledWithNewSchedule.addSchedule(
                new CourseSchedule(CourseDayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)));

        // when
        boolean changed = original.hasMetadataOrScheduleChanged(crawledWithNewSchedule);
        boolean updated = original.updateMetadata(crawledWithNewSchedule);

        // then
        assertThat(changed).isTrue();
        assertThat(updated).isTrue();
        assertThat(original.getSchedules()).hasSize(1);
    }

    private Course createSampleCourse(String courseKey, int capacity, int current) {
        return Course.builder()
                .courseKey(courseKey)
                .subjectCode("COMP101")
                .name("컴퓨터프로그래밍")
                .classNumber("01")
                .professor("홍길동")
                .capacity(capacity)
                .current(current)
                .academicYear("2026")
                .semester("U211600010")
                .department("컴퓨터공학부")
                .classTime("월 09:00-11:00")
                .classroom("7호관 101호")
                .hasSyllabus(true)
                .build();
    }
}
