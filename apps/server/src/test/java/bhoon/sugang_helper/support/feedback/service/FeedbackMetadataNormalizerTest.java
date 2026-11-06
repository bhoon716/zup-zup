package bhoon.sugang_helper.support.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FeedbackMetadataNormalizerTest {

    private final FeedbackMetadataNormalizer normalizer = new FeedbackMetadataNormalizer(new ObjectMapper());

    @Test
    @DisplayName("환경 메타데이터는 허용된 선택 필드만 정해진 순서로 보존한다")
    void preservesOnlyAllowedOptionalFieldsInFixedOrder() {
        // when & then
        assertThat(normalizer.normalize("{\"language\":\" ko-KR \",\"os\":\" MacIntel \"}"))
                .isEqualTo("{\"os\":\"MacIntel\",\"language\":\"ko-KR\"}");
        assertThat(normalizer.normalize("{}")).isEqualTo("{}");
        assertThat(normalizer.normalize(null)).isEqualTo("{}");
    }

    @ParameterizedTest
    @DisplayName("잘못된 JSON이나 스키마 밖의 환경 메타데이터를 거부한다")
    @ValueSource(strings = {
            "{",
            "[]",
            "{\"url\":\"https://private.example\"}",
            "{\"os\":false}",
            "{\"os\":\"first\",\"os\":\"second\"}",
            "{\"os\":\"MacIntel\"} {}"
    })
    void rejectsMalformedOrOutOfSchemaMetadata(String metadata) {
        // when & then
        assertInvalid(metadata);
    }

    @Test
    @DisplayName("크기 제한을 넘는 환경 메타데이터와 필드를 거부한다")
    void rejectsOversizedMetadataAndFields() {
        // when & then
        assertInvalid(" ".repeat(513));
        assertInvalid("{\"os\":\"" + "o".repeat(129) + "\"}");
        assertInvalid("{\"language\":\"" + "l".repeat(36) + "\"}");
    }

    private void assertInvalid(String metadata) {
        assertThatThrownBy(() -> normalizer.normalize(metadata))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }
}
