package bhoon.sugang_helper.admin.application;

/**
 * 원문을 저장하지 않고 변경 여부를 비교할 수 있도록 남기는 내용 요약입니다.
 */
public record ContentSummary(int length, String fingerprint) {
}
