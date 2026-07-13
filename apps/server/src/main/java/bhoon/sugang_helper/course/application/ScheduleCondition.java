package bhoon.sugang_helper.course.application;

import bhoon.sugang_helper.course.domain.CourseDayOfWeek;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "시간표 검색 조건")
public class ScheduleCondition {
    @Schema(description = "요일", example = "MONDAY")
    @NotNull
    private CourseDayOfWeek dayOfWeek;

    @Schema(description = "시작 시간", example = "09:00:00")
    @Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?")
    private String startTime;

    @Schema(description = "종료 시간", example = "10:00:00")
    @Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?")
    private String endTime;
}
