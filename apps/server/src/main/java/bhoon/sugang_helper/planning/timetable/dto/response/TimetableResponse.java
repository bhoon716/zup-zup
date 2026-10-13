package bhoon.sugang_helper.planning.timetable.dto.response;

import bhoon.sugang_helper.planning.timetable.entity.Timetable;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimetableResponse {
    private Long id;
    private String name;
    private boolean isPrimary;
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
