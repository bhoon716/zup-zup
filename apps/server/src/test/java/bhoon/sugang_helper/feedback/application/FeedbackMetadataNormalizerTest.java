package bhoon.sugang_helper.feedback.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FeedbackMetadataNormalizerTest {

    private final FeedbackMetadataNormalizer normalizer = new FeedbackMetadataNormalizer(new ObjectMapper());

    @Test
    void preservesOnlyAllowedOptionalFieldsInFixedOrder() {
        assertThat(normalizer.normalize("{\"language\":\" ko-KR \",\"os\":\" MacIntel \"}"))
                .isEqualTo("{\"os\":\"MacIntel\",\"language\":\"ko-KR\"}");
        assertThat(normalizer.normalize("{}")).isEqualTo("{}");
        assertThat(normalizer.normalize(null)).isEqualTo("{}");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{",
            "[]",
            "{\"url\":\"https://private.example\"}",
            "{\"os\":false}",
            "{\"os\":\"first\",\"os\":\"second\"}",
            "{\"os\":\"MacIntel\"} {}"
    })
    void rejectsMalformedOrOutOfSchemaMetadata(String metadata) {
        assertInvalid(metadata);
    }

    @Test
    void rejectsOversizedMetadataAndFields() {
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
