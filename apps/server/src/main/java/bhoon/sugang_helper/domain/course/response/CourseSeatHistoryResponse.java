package bhoon.sugang_helper.domain.course.response;

import bhoon.sugang_helper.domain.course.entity.CourseSeatHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "과목 공석 이력 응답 DTO")
public class CourseSeatHistoryResponse {
    @Schema(description = "이력 ID", example = "1")
    private Long id;

    @Schema(description = "과목 키", example = "0000130844-1")
    private String courseKey;

    @Schema(description = "당시 정원", example = "40")
    private Integer capacity;

    @Schema(description = "당시 현재 인원", example = "35")
    private Integer current;

    @Schema(description = "감지 시간", example = "2024-01-01T12:00:00")
    private LocalDateTime detectedAt;

    public static CourseSeatHistoryResponse from(CourseSeatHistory history) {
        return CourseSeatHistoryResponse.builder()
                .id(history.getId())
                .courseKey(history.getCourseKey())
                .capacity(history.getCapacity())
                .current(history.getCurrent())
                .detectedAt(history.getCreatedAt())
                .build();
    }
}
