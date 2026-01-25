package bhoon.sugang_helper.domain.course.request;

import bhoon.sugang_helper.domain.course.enums.ClassPeriod;
import bhoon.sugang_helper.domain.course.enums.CourseDayOfWeek;
import io.swagger.v3.oas.annotations.media.Schema;
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
    private CourseDayOfWeek dayOfWeek;

    @Schema(description = "교시", example = "PERIOD_1A")
    private ClassPeriod period;
}
