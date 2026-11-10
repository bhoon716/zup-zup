package bhoon.sugang_helper.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminHourlyTrafficResponse {

    private final String label;

    private final long count;

    public static AdminHourlyTrafficResponse of(String label, long count) {
        return new AdminHourlyTrafficResponse(label, count);
    }
}
