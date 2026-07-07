package bhoon.sugang_helper.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewScopeKeyTest {

    @Test
    @DisplayName("교수명이 없거나 '교수 미지정'이면 빈 문자열로 정규화한다")
    void normalizeProfessor_BlankOrPlaceholder() {
        assertThat(ReviewScopeKey.normalizeProfessor(null)).isEqualTo("");
        assertThat(ReviewScopeKey.normalizeProfessor("   ")).isEqualTo("");
        assertThat(ReviewScopeKey.normalizeProfessor("교수 미지정")).isEqualTo("");
    }
}
