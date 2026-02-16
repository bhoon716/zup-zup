package bhoon.sugang_helper.domain.timetable.response;

import bhoon.sugang_helper.domain.timetable.entity.Timetable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "시간표 목록 응답 DTO")
public class TimetableResponse {
    @Schema(description = "시간표 ID", example = "1")
    private Long id;
    @Schema(description = "시간표 이름", example = "2026-1학기")
    private String name;
    @Schema(description = "대표 시간표 여부", example = "true")
    private boolean isPrimary;
    @Schema(description = "담긴 강의 수", example = "5")
    private long courseCount;

    public static TimetableResponse of(Timetable timetable) {
        return TimetableResponse.builder()
                .id(timetable.getId())
                .name(timetable.getName())
                .isPrimary(timetable.isPrimary())
                .courseCount(timetable.getEntries().size())
                .build();
    }
}
