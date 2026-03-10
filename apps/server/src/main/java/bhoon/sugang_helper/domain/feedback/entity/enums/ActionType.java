package bhoon.sugang_helper.domain.feedback.entity.enums;

/**
 * 관리자가 수행한 액션의 종류를 정의하는 열거형입니다.
 * 로그 기록 및 히스토리 관리 시 사용됩니다.
 */
public enum ActionType {
    STATUS_CHANGE, // 처리 상태 변경
    REPLY_CREATE, // 운영진 답변 등록
    REPLY_UPDATE, // 운영진 답변 수정
    REPLY_DELETE, // 운영진 답변 삭제
    FEEDBACK_DELETE // 문의글 삭제
}
