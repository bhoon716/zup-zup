package bhoon.sugang_helper.academic.course.enums;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SemesterType {
    FIRST_SEMESTER("U211600010", "SUSR016.010", "1학기"),
    SECOND_SEMESTER("U211600020", "SUSR016.020", "2학기"),
    SUMMER_SESSION("U211600015", "SUSR016.015", "하기 계절학기"),
    WINTER_SESSION("U211600025", "SUSR016.025", "동기 계절학기"),
    SUMMER_SPECIAL("U211600016", "SUSR016.016", "여름 특별학기"),
    WINTER_SPECIAL("U211600026", "SUSR016.026", "겨울 특별학기"),
    FRESHMAN_SPECIAL("U211600009", "SUSR016.009", "신입생 특별학기"),
    SW_SPECIAL("U211600008", "SUSR016.008", "SW 특별학기");

    private final String code;
    private final String jumpCode;
    private final String description;

    public static String toJumpCode(String code) {
        return Arrays.stream(values())
                .filter(semester -> code.equals(semester.code))
                .map(SemesterType::getJumpCode)
                .findFirst()
                .orElse(code);
    }

    public static SemesterType fromCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "유효하지 않은 학기 코드입니다: " + code));
    }
}
