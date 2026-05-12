package bhoon.sugang_helper.domain.course.event;

/**
 * 강의의 빈자리가 발생했을 때 발행되는 이벤트 객체입니다.
 */
public record SeatOpenedEvent(
        String courseKey,
        String courseName,
        String professor,
        Integer previousSeats,
        Integer currentSeats) {
}
