package bhoon.sugang_helper.domain.course.enums;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Arrays;

/**
 * 전북대학교 학기 구분 코드와 명칭을 관리하는 열거형입니다.
 */
@Getter
@RequiredArgsConstructor
public enum SemesterType {
    FIRST_SEMESTER("U211600010", "1학기"),
    SECOND_SEMESTER("U211600020", "2학기"),
    SUMMER_SESSION("U211600015", "하기 계절학기"),
    WINTER_SESSION("U211600025", "동기 계절학기"),
    SUMMER_SPECIAL("U211600016", "여름 특별학기"),
    WINTER_SPECIAL("U211600026", "겨울 특별학기"),
    FRESHMAN_SPECIAL("U211600009", "신입생 특별학기"),
    SW_SPECIAL("U211600008", "SW 특별학기");

    private final String code;
    private final String description;

    /**
     * 학기 코드를 통해 해당하는 SemesterType을 반환합니다.
     */
    public static SemesterType fromCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "유효하지 않은 학기 코드입니다: " + code));
    }
}
