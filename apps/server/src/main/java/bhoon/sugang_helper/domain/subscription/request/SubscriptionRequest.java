package bhoon.sugang_helper.domain.subscription.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Schema(description = "구독 신청 요청 DTO")
public class SubscriptionRequest {

    @NotBlank(message = "과목 코드는 필수입니다.")
    @Schema(description = "과목 키 (과목코드-분반)", example = "0000130844-1")
    private String courseKey;
}
