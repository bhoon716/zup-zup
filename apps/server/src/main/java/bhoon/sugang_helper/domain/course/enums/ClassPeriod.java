package bhoon.sugang_helper.domain.course.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClassPeriod {
    PERIOD_0A("0-A"), PERIOD_0B("0-B"),
    PERIOD_1A("1-A"), PERIOD_1B("1-B"),
    PERIOD_2A("2-A"), PERIOD_2B("2-B"),
    PERIOD_3A("3-A"), PERIOD_3B("3-B"),
    PERIOD_4A("4-A"), PERIOD_4B("4-B"),
    PERIOD_5A("5-A"), PERIOD_5B("5-B"),
    PERIOD_6A("6-A"), PERIOD_6B("6-B"),
    PERIOD_7A("7-A"), PERIOD_7B("7-B"),
    PERIOD_8A("8-A"), PERIOD_8B("8-B"),
    PERIOD_9A("9-A"), PERIOD_9B("9-B"),
    PERIOD_10A("10-A"), PERIOD_10B("10-B"),
    PERIOD_11A("11-A"), PERIOD_11B("11-B"),
    PERIOD_12A("12-A"), PERIOD_12B("12-B"),
    PERIOD_13A("13-A"), PERIOD_13B("13-B"),
    PERIOD_14A("14-A"), PERIOD_14B("14-B"),
    PERIOD_15A("15-A"), PERIOD_15B("15-B");

    private final String description;

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
