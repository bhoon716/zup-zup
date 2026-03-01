package bhoon.sugang_helper.domain.course.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseStatus {
    GENERAL("일반"),
    REMOTE_CONTENTS("원격강좌(콘텐츠)"),
    REMOTE_REALTIME("원격강좌(실시간)"),
    FLIPPED_LEARNING("플립러닝"),
    BLENDED("블렌디드러닝"),
    ONLINE_OFFLINE("온·오프라인강좌"),
    FIELD_TRAINING("현장실습"),
    SOCIAL_SERVICE("사회봉사"),
    THESIS_RESEARCH("논문연구"),
    VIDEO_CONFERENCE("화상강의"),
    SPECIAL_ENGLISH("특별(영어)");

    private final String description;

    public static CourseStatus from(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(s -> s.description.trim().equalsIgnoreCase(description.trim()))
                .findFirst()
                .orElse(null); // 매핑되지 않는 값은 null 처리 (필요하면 미확인 상수 추가)
    }
}
