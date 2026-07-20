package bhoon.sugang_helper.feedback.domain;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import java.util.Locale;

/**
 * 관리자 피드백 목록의 삭제 상태 필터입니다.
 */
public enum AdminFeedbackDeletionFilter {
    ALL,
    ACTIVE,
    DELETED;

    public static AdminFeedbackDeletionFilter from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "삭제 상태 필터가 올바르지 않습니다.");
        }
    }
}
