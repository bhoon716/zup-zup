package bhoon.sugang_helper.domain.subscription.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class SubscriptionRequest {

    @NotBlank(message = "과목 코드는 필수입니다.")
    private String courseKey;
}
