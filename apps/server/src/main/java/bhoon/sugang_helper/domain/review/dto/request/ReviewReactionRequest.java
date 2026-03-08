package bhoon.sugang_helper.domain.review.dto.request;

import bhoon.sugang_helper.domain.review.enums.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "리뷰 공감/비공감 투표 요청")
public record ReviewReactionRequest(
                @NotNull(message = "반응 타입은 필수입니다. (LIKE 또는 DISLIKE)") @Schema(description = "공감/비공감 여부", example = "LIKE") ReactionType reactionType) {
}
