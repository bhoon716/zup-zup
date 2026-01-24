package bhoon.sugang_helper.domain.subscription.response;

import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "구독 상세 정보 응답 DTO")
public class SubscriptionResponse {

    @Schema(description = "구독 ID", example = "1")
    private final Long id;

    @Schema(description = "과목 키", example = "0000130844-1")
    private final String courseKey;

    @Schema(description = "과목명", example = "(글로컬)우리생활과화학")
    private final String courseName;

    @Schema(description = "담당 교수", example = "김혜진")
    private final String professor;

    @Schema(description = "알림 활성화 여부", example = "true")
    private final boolean isActive;

    @Schema(description = "구독 생성 일시", example = "2024-01-01T12:00:00")
    private final LocalDateTime createdAt;

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getCourseKey(),
                null, // 서비스에서 과목명 조회 후 매핑 예정
                null, // 서비스에서 교수명 조회 후 매핑 예정
                subscription.isActive(),
                subscription.getCreatedAt());
    }

    public static SubscriptionResponse of(Subscription subscription, String courseName, String professor) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getCourseKey(),
                courseName,
                professor,
                subscription.isActive(),
                subscription.getCreatedAt());
    }
}
