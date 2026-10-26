package bhoon.sugang_helper.planning.timetable.dto.response;

import bhoon.sugang_helper.planning.timetable.entity.Timetable;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimetableDetailResponse {
    private Long id;
    private String name;
    private boolean isPrimary;
    private List<TimetableCourseResponse> courses;
    private List<CustomScheduleResponse> customSchedules;
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
