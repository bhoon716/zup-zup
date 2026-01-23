package bhoon.sugang_helper.domain.course.event;

public record SeatOpenedEvent(
        String courseKey,
        String courseName,
        Integer previousSeats,
        Integer currentSeats) {
}
