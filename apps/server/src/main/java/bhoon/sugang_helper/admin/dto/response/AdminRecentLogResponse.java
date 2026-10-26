package bhoon.sugang_helper.admin.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminRecentLogResponse {

    private LocalDateTime timestamp;

    private String level;

    private String message;

    private String source;
}
