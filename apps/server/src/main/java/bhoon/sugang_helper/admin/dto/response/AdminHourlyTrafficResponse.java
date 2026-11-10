package bhoon.sugang_helper.admin.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminHourlyTrafficResponse {

    private static final DateTimeFormatter HOUR_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final String label;

    private final long count;

    public static AdminHourlyTrafficResponse of(LocalDateTime hour, long count) {
        return new AdminHourlyTrafficResponse(hour.format(HOUR_LABEL_FORMATTER), count);
    }
}
