package bhoon.sugang_helper.domain.review.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "강의 리뷰 수정 요청")
public record ReviewUpdateRequest(

        @Schema(description = "별점 (1~5)", example = "4") @Min(value = 1, message = "별점은 최소 1점 이상이어야 합니다.") @Max(value = 5, message = "별점은 최대 5점 이하여야 합니다.") int rating) {}
