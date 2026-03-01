package bhoon.sugang_helper.domain.admin.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "시간대별 트래픽 응답 DTO")
public class AdminHourlyTrafficResponse {

    @Schema(description = "시간 라벨(HH:mm)", example = "14:00")
    private String label;

    @Schema(description = "해당 시간대 알림 건수", example = "12")
    private long count;
}
