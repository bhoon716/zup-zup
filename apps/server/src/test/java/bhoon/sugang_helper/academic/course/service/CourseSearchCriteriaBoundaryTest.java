package bhoon.sugang_helper.academic.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.academic.course.dto.criteria.CourseSearchCriteria;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CourseSearchCriteriaBoundaryTest {

    @Test
    @DisplayName("저장소 조건 확장 전에 수업 운영 방식을 정규화하고 중복을 제거한다")
    void normalizesAndDeduplicatesCourseDirectionTermsBeforeRepositoryExpansion() {
        // given
        CourseSearchCriteria criteria = CourseSearchCriteria.builder()
                .courseDirectionTerms(List.of(" 대면수업 ", "실시간", "대면수업"))
                .build();

        // when & then
        assertThat(criteria.courseDirectionTerms()).containsExactly("대면수업", "실시간");
    }

    @Test
    @DisplayName("검색 조건 제한을 넘겨 확장될 수 있는 수업 운영 방식을 거부한다")
    void rejectsCourseDirectionTermsThatCouldExpandBeyondThePredicateLimit() {
        // given
        List<String> tooManyTerms = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");

        // when & then
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
    @DisplayName("IN 조건 제한을 넘겨 확장될 수 있는 설강 상태를 거부한다")
    void rejectsStatusesThatCouldExpandBeyondTheInPredicateLimit() {
        // given
        List<String> tooManyStatuses = IntStream.rangeClosed(1, 21)
                .mapToObj(index -> "status-" + index)
                .toList();

        // when & then
        assertThatThrownBy(() -> CourseSearchCriteria.builder()
                .statuses(tooManyStatuses)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statuses");
    }
}
