package bhoon.sugang_helper.academic.course.dto.request;

import bhoon.sugang_helper.academic.course.enums.CourseDayOfWeek;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ScheduleCondition {

    private static final String TIME_PATTERN = "(?:[01]\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?";

    @NotNull
    private CourseDayOfWeek dayOfWeek;

    @Pattern(regexp = TIME_PATTERN)
    private String startTime;

    @Pattern(regexp = TIME_PATTERN)
    private String endTime;
}
