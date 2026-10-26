package bhoon.sugang_helper.academic.course.event;

/**
 * 강의의 빈자리 발생 정보를 outbox와 발송 경로에 전달합니다.
 */
public record SeatOpenedEvent(
        String courseKey,
        String courseName,
        String professor,
        Integer previousSeats,
        Integer currentSeats) {
}
