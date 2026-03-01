package bhoon.sugang_helper.domain.course.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GradingMethod {
    PASS_FAIL("Pass/Fail"),
    ETC_LAW_SCHOOL("기타(법전원)"),
    RELATIVE_1("상대평가Ⅰ"),
    RELATIVE_2("상대평가Ⅱ"),
    RELATIVE_3("상대평가Ⅲ"),
    ABSOLUTE("절대평가");

    private final String description;

    public static GradingMethod from(String description) {
        return Arrays.stream(values())
                .filter(g -> g.description.equals(description))
                .findFirst()
                .orElse(null);
    }
}
