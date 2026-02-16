package bhoon.sugang_helper.domain.notification.response;

import bhoon.sugang_helper.domain.notification.entity.NotificationHistory;
import bhoon.sugang_helper.domain.notification.sender.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "알림 수신 내역 응답 DTO")
public class NotificationHistoryResponse {
    @Schema(description = "알림 ID", example = "1")
    private Long id;

    @Schema(description = "과목 키", example = "0000130844-1")
    private String courseKey;

    @Schema(description = "알림 제목", example = "공석 발생")
    private String title;

    @Schema(description = "알림 내용", example = "[(글로컬)우리생활과화학] 과목에 공석이 발생했습니다.")
    private String message;

    @Schema(description = "알림 채널 (FCM, EMAIL, WEB)", example = "FCM")
    private NotificationChannel channel;

    @Schema(description = "발송 시간", example = "2024-01-01T12:00:00")
    private LocalDateTime sentAt;

    public static NotificationHistoryResponse from(NotificationHistory history) {
        return NotificationHistoryResponse.builder()
                .id(history.getId())
                .courseKey(history.getCourseKey())
                .title(history.getTitle())
                .message(history.getMessage())
                .channel(history.getChannel())
                .sentAt(history.getCreatedAt())
                .build();
    }
}
