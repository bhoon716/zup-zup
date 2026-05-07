package bhoon.sugang_helper.domain.review.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.Length;

@Schema(description = "강의 리뷰 수정 요청")
public record ReviewUpdateRequest(

                @Schema(description = "별점 (1~5)", example = "4") @Min(value = 1, message = "별점은 최소 1점 이상이어야 합니다.") @Max(value = 5, message = "별점은 최대 5점 이하여야 합니다.") int rating,

                @Schema(description = "수정할 리뷰 코멘트 (255자 이내)", example = "강의 내용이 조금 아쉬웠습니다.") @Length(max = 255, message = "리뷰 내용은 255자를 초과할 수 없습니다.") String content) {
        public ReviewUpdateRequest {
                content = normalizeContent(content);
        }

        private static String normalizeContent(String content) {
                if (content == null || content.isBlank()) {
                        return null;
                }
                return content.trim();
        }
}
