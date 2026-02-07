package bhoon.sugang_helper.domain.timetable.response;

import bhoon.sugang_helper.domain.timetable.entity.Timetable;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

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
