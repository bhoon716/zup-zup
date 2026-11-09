package bhoon.sugang_helper.academic.events;

public record SeatOpenedEvent(
        String courseKey,
        String courseName,
        String professor,
        Integer previousSeats,
        Integer currentSeats) {
}
