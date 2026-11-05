package bhoon.sugang_helper.identity.events;

/** 같은 트랜잭션에서 사용자 소유 데이터의 탈퇴 처리를 완료한다. */
public record UserWithdrawnEvent(Long userId) {
}
