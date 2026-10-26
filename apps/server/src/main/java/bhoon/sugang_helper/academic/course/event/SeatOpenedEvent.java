package bhoon.sugang_helper.academic.course.event;




public record SeatOpenedEvent(
        String courseKey,
        String courseName,
        String professor,
        Integer previousSeats,
        Integer currentSeats) {
}
