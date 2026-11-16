package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.seatalert.api.NotificationTraffic;
import bhoon.sugang_helper.seatalert.notification.repository.NotificationTrafficRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationTrafficService implements NotificationTraffic {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter HOUR_LABEL = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("M/d");

    private final NotificationTrafficRepository repository;
    private final Clock clock;

    @Autowired
    public NotificationTrafficService(NotificationTrafficRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    NotificationTrafficService(NotificationTrafficRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public List<Bucket> findRecent(Range range) {
        Window window = switch (range) {
            case LAST_24_HOURS -> new Window(24, ChronoUnit.HOURS, HOUR_LABEL);
            case LAST_7_DAYS -> new Window(7, ChronoUnit.DAYS, DAY_LABEL);
        };
        var now = clock.instant();
        var firstBucket = now.atZone(SEOUL).truncatedTo(window.unit()).minus(window.size() - 1, window.unit());
        var storedStart = LocalDateTime.ofInstant(firstBucket.toInstant(), clock.getZone());
        var storedNow = LocalDateTime.ofInstant(now, clock.getZone());
        double bucketSeconds = window.unit().getDuration().toSeconds();

        long[] seatOpened = new long[window.size()];
        long[] feedbackReply = new long[window.size()];
        long[] announcement = new long[window.size()];
        repository.countSeatDeliveries(storedStart, storedNow, bucketSeconds)
                .forEach(row -> seatOpened[row.getBucket()] = row.getDeliveryCount());
        for (var row : repository.countInboxDeliveries(storedStart, storedNow, bucketSeconds)) {
            feedbackReply[row.getBucket()] = row.getFeedbackReply();
            announcement[row.getBucket()] = row.getAnnouncement();
        }

        return IntStream.range(0, window.size())
                .mapToObj(index -> {
                    var start = firstBucket.plus(index, window.unit());
                    return new Bucket(start.format(window.label()), start.toOffsetDateTime(),
                            seatOpened[index], feedbackReply[index], announcement[index]);
                })
                .toList();
    }

    private record Window(int size, ChronoUnit unit, DateTimeFormatter label) {}
}
