package bhoon.sugang_helper.domain.course.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum LectureType {
    FACE_TO_FACE("대면중심수업(70%미만 온라인)"),
    NON_FACE_TO_FACE("비대면수업"), // 혹시 몰라 추가
    BLENDED("혼합수업"); // 혹시 몰라 추가

    private final String description;

    public static LectureType from(String description) {
        if (description == null)
            return null;
        return Arrays.stream(values())
                .filter(l -> l.description.equals(description))
                .findFirst()
                .orElse(null);
    }
}
