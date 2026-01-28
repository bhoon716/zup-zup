package bhoon.sugang_helper.domain.course.enums;

import java.time.LocalTime;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClassPeriod {
    PERIOD_0A("0-A", LocalTime.of(8, 0), LocalTime.of(8, 30)),
    PERIOD_0B("0-B", LocalTime.of(8, 30), LocalTime.of(9, 0)),
    PERIOD_1A("1-A", LocalTime.of(9, 0), LocalTime.of(9, 30)),
    PERIOD_1B("1-B", LocalTime.of(9, 30), LocalTime.of(10, 0)),
    PERIOD_2A("2-A", LocalTime.of(10, 0), LocalTime.of(10, 30)),
    PERIOD_2B("2-B", LocalTime.of(10, 30), LocalTime.of(11, 0)),
    PERIOD_3A("3-A", LocalTime.of(11, 0), LocalTime.of(11, 30)),
    PERIOD_3B("3-B", LocalTime.of(11, 30), LocalTime.of(12, 0)),
    PERIOD_4A("4-A", LocalTime.of(12, 0), LocalTime.of(12, 30)),
    PERIOD_4B("4-B", LocalTime.of(12, 30), LocalTime.of(13, 0)),
    PERIOD_5A("5-A", LocalTime.of(13, 0), LocalTime.of(13, 30)),
    PERIOD_5B("5-B", LocalTime.of(13, 30), LocalTime.of(14, 0)),
    PERIOD_6A("6-A", LocalTime.of(14, 0), LocalTime.of(14, 30)),
    PERIOD_6B("6-B", LocalTime.of(14, 30), LocalTime.of(15, 0)),
    PERIOD_7A("7-A", LocalTime.of(15, 0), LocalTime.of(15, 30)),
    PERIOD_7B("7-B", LocalTime.of(15, 30), LocalTime.of(16, 0)),
    PERIOD_8A("8-A", LocalTime.of(16, 0), LocalTime.of(16, 30)),
    PERIOD_8B("8-B", LocalTime.of(16, 30), LocalTime.of(17, 0)),
    PERIOD_9A("9-A", LocalTime.of(17, 0), LocalTime.of(17, 30)),
    PERIOD_9B("9-B", LocalTime.of(17, 30), LocalTime.of(18, 0)),
    PERIOD_10A("10-A", LocalTime.of(18, 0), LocalTime.of(18, 30)),
    PERIOD_10B("10-B", LocalTime.of(18, 30), LocalTime.of(19, 0)),
    PERIOD_11A("11-A", LocalTime.of(19, 0), LocalTime.of(19, 30)),
    PERIOD_11B("11-B", LocalTime.of(19, 30), LocalTime.of(20, 0)),
    PERIOD_12A("12-A", LocalTime.of(20, 0), LocalTime.of(20, 30)),
    PERIOD_12B("12-B", LocalTime.of(20, 30), LocalTime.of(21, 0)),
    PERIOD_13A("13-A", LocalTime.of(21, 0), LocalTime.of(21, 30)),
    PERIOD_13B("13-B", LocalTime.of(21, 30), LocalTime.of(22, 0)),
    PERIOD_14A("14-A", LocalTime.of(22, 0), LocalTime.of(22, 30)),
    PERIOD_14B("14-B", LocalTime.of(22, 30), LocalTime.of(23, 0)),
    PERIOD_15A("15-A", LocalTime.of(23, 0), LocalTime.of(23, 30)),
    PERIOD_15B("15-B", LocalTime.of(23, 30), LocalTime.of(23, 59));

    private final String description;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public static ClassPeriod from(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(p -> p.description.equalsIgnoreCase(description.trim()))
                .findFirst()
                .orElse(null);
    }
}
