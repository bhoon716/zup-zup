package bhoon.sugang_helper.domain.admin.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 최근 로그 응답 DTO")
public class AdminRecentLogResponse {

    @Schema(description = "로그 시각", example = "2026-02-20T14:32:05")
    private LocalDateTime timestamp;

    @Schema(description = "로그 레벨", example = "INFO")
    private String level;

    @Schema(description = "로그 메시지", example = "배치 알림 발송 완료")
    private String message;

    @Schema(description = "로그 소스", example = "NotificationService")
    private String source;
}
