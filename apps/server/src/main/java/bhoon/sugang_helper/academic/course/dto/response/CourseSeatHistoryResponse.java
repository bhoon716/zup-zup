package bhoon.sugang_helper.academic.course.dto.response;

import bhoon.sugang_helper.academic.course.entity.CourseSeatHistory;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseSeatHistoryResponse {
    private final Long id;

    private final String courseKey;

    private final Integer capacity;

    private final Integer current;

    private final LocalDateTime detectedAt;

    public static CourseSeatHistoryResponse from(CourseSeatHistory history) {
        return new CourseSeatHistoryResponse(
                history.getId(),
                history.getCourseKey(),
                history.getCapacity(),
                history.getCurrent(),
                history.getCreatedAt());
    }
}
