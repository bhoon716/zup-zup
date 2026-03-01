package bhoon.sugang_helper.domain.timetable.response;

import bhoon.sugang_helper.domain.timetable.entity.Timetable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "시간표 상세 응답 DTO")
public class TimetableDetailResponse {
    @Schema(description = "시간표 ID", example = "1")
    private Long id;
    @Schema(description = "시간표 이름", example = "2026-1학기")
    private String name;
    @Schema(description = "대표 시간표 여부", example = "true")
    private boolean isPrimary;
    @Schema(description = "시간표에 포함된 강의 목록")
    private List<TimetableCourseResponse> courses;
    @Schema(description = "커스텀 일정 목록")
    private List<CustomScheduleResponse> customSchedules;
    @Schema(description = "총 학점", example = "15.0")
    private String totalCredits;

    public static TimetableDetailResponse of(Timetable timetable, List<TimetableCourseResponse> courses,
                                             String totalCredits) {
        return TimetableDetailResponse.builder()
                .id(timetable.getId())
                .name(timetable.getName())
                .isPrimary(timetable.isPrimary())
                .courses(courses)
                .customSchedules(timetable.getCustomSchedules().stream()
                        .map(CustomScheduleResponse::of)
                        .toList())
                .totalCredits(totalCredits)
                .build();
    }
}
