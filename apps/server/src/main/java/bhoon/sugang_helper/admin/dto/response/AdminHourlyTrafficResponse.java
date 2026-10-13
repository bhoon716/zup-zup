package bhoon.sugang_helper.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminHourlyTrafficResponse {

    private String label;

    private long count;
}
