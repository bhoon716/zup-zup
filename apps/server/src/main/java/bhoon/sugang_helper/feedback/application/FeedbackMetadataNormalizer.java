package bhoon.sugang_helper.feedback.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Stores only the small, non-identifying environment metadata needed for feedback triage.
 */
@Component
public class FeedbackMetadataNormalizer {

    private static final int MAX_METADATA_BYTES = 512;
    private static final int MAX_OS_LENGTH = 128;
    private static final int MAX_LANGUAGE_LENGTH = 35;
    private static final Set<String> ALLOWED_FIELDS = Set.of("os", "language");

    private final ObjectMapper objectMapper;

    public FeedbackMetadataNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validates the client-provided JSON and serializes allowed fields in a deterministic order.
     */
    public String normalize(String metadata) {
        if (metadata == null) {
            return "{}";
        }
        if (exceedsMaximumBytes(metadata)) {
            throw invalidMetadata();
        }

        JsonNode parsed = parse(metadata);
        if (!parsed.isObject()) {
            throw invalidMetadata();
        }

        ObjectNode object = (ObjectNode) parsed;
        object.fieldNames().forEachRemaining(field -> {
            if (!ALLOWED_FIELDS.contains(field)) {
                throw invalidMetadata();
            }
        });

        String os = readOptionalText(object, "os", MAX_OS_LENGTH);
        String language = readOptionalText(object, "language", MAX_LANGUAGE_LENGTH);

        ObjectNode normalized = objectMapper.createObjectNode();
        if (os != null) {
            normalized.put("os", os);
        }
        if (language != null) {
            normalized.put("language", language);
        }

        String serialized = normalized.toString();
        if (exceedsMaximumBytes(serialized)) {
            throw invalidMetadata();
        }
        return serialized;
    }

    private JsonNode parse(String metadata) {
        try (JsonParser parser = objectMapper.getFactory().createParser(metadata)) {
            parser.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            JsonNode parsed = objectMapper.readTree(parser);
            if (parsed == null || parser.nextToken() != null) {
                throw invalidMetadata();
            }
            return parsed;
        } catch (IOException exception) {
            throw invalidMetadata();
        }
    }

    private String readOptionalText(ObjectNode metadata, String field, int maximumLength) {
        JsonNode value = metadata.get(field);
        if (value == null) {
            return null;
        }
        if (!value.isTextual()) {
            throw invalidMetadata();
        }

        String normalized = value.textValue().strip();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw invalidMetadata();
        }
        return normalized;
    }

    private int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private boolean exceedsMaximumBytes(String value) {
        return value.length() > MAX_METADATA_BYTES || utf8Length(value) > MAX_METADATA_BYTES;
    }

    private CustomException invalidMetadata() {
        return new CustomException(ErrorCode.INVALID_INPUT, "피드백 환경 정보가 유효하지 않습니다.");
    }
}
