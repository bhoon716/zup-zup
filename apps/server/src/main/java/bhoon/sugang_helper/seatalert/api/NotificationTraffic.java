package bhoon.sugang_helper.seatalert.api;

import java.time.OffsetDateTime;
import java.util.List;

public interface NotificationTraffic {

    List<Bucket> findRecent(Range range);

    long countSentSeatNotifications();

    enum Range {
        LAST_24_HOURS, LAST_7_DAYS
    }

    record Bucket(String label, OffsetDateTime bucketStart, long seatOpened, long feedbackReply, long announcement) {}
}
