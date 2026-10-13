package bhoon.sugang_helper.academic.review.dto.request;

import bhoon.sugang_helper.academic.review.enums.ReactionType;
import jakarta.validation.constraints.NotNull;

public record ReviewReactionRequest(
        @NotNull(message = "반응 타입은 필수입니다. (LIKE 또는 DISLIKE)") ReactionType reactionType) {
}
