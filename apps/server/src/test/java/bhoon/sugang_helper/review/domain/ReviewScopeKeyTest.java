package bhoon.sugang_helper.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewScopeKeyTest {

    @Test
    @DisplayName("교수명이 없거나 교수 미지정이면 빈 문자열로 정규화한다")
    void normalizeProfessor_BlankOrUnassigned() {
        assertThat(ReviewScopeKey.normalizeProfessor(null)).isEqualTo("");
        assertThat(ReviewScopeKey.normalizeProfessor("   ")).isEqualTo("");
        assertThat(ReviewScopeKey.normalizeProfessor("교수 미지정")).isEqualTo("");
    }

    @Test
    @DisplayName("실제 교수명이 있을 때만 학기 간 리뷰 범위로 묶는다")
    void hasIdentifiableProfessor_OnlyForNamedProfessor() {
        assertThat(new ReviewScopeKey("CSE101", "김교수").hasIdentifiableProfessor()).isTrue();
        assertThat(new ReviewScopeKey("CSE101", "").hasIdentifiableProfessor()).isFalse();
    }

    @Test
    @DisplayName("외부교원N은 학기 간 공유에 사용할 수 있는 교수 식별자로 유지한다")
    void normalizeProfessor_KeepsExternalInstructorIdentifier() {
        String professor = ReviewScopeKey.normalizeProfessor("외부교원1");

        assertThat(professor).isEqualTo("외부교원1");
        assertThat(new ReviewScopeKey("CSE101", professor).hasIdentifiableProfessor()).isTrue();
    }
}
