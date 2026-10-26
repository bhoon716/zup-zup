package bhoon.sugang_helper.academic.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ReviewUpdateRequest(

        @Min(value = 1, message = "별점은 최소 1점 이상이어야 합니다.") @Max(value = 5, message = "별점은 최대 5점 이하여야 합니다.") int rating) {
}
