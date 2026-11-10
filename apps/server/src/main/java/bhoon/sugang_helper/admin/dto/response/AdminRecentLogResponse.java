package bhoon.sugang_helper.admin.dto.response;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminRecentLogResponse {

    private final LocalDateTime timestamp;

    private final String level;

    private final String message;

    private final String source;

    public static AdminRecentLogResponse of(LocalDateTime timestamp, String level, String message, String source) {
        return new AdminRecentLogResponse(timestamp, level, message, source);
    }
}
