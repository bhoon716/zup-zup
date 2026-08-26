package bhoon.sugang_helper.course.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class CourseSearchCriteriaBoundaryTest {

    @Test
    void normalizesAndDeduplicatesCourseDirectionTermsBeforeRepositoryExpansion() {
        CourseSearchCriteria criteria = CourseSearchCriteria.builder()
                .courseDirectionTerms(List.of(" 대면수업 ", "실시간", "대면수업"))
                .build();

        assertThat(criteria.courseDirectionTerms()).containsExactly("대면수업", "실시간");
    }

    @Test
    void rejectsCourseDirectionTermsThatCouldExpandBeyondThePredicateLimit() {
        List<String> tooManyTerms = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");

        assertThatThrownBy(() -> CourseSearchCriteria.builder()
                .courseDirectionTerms(tooManyTerms)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("courseDirection");
        assertThatThrownBy(() -> CourseSearchCriteria.builder()
                .courseDirectionTerms(List.of("x".repeat(101)))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("courseDirection");
    }

    @Test
    void rejectsStatusesThatCouldExpandBeyondTheInPredicateLimit() {
        List<String> tooManyStatuses = java.util.stream.IntStream.rangeClosed(1, 21)
                .mapToObj(index -> "status-" + index)
                .toList();

        assertThatThrownBy(() -> CourseSearchCriteria.builder()
                .statuses(tooManyStatuses)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statuses");
    }
}
