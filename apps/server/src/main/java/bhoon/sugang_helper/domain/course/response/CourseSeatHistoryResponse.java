package bhoon.sugang_helper.domain.course.response;

import bhoon.sugang_helper.domain.course.entity.CourseSeatHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CourseSeatHistoryResponse {
    private Long id;
    private String courseKey;
    private Integer capacity;
    private Integer current;
    private LocalDateTime detectedAt;

    public static CourseSeatHistoryResponse from(CourseSeatHistory history) {
        return CourseSeatHistoryResponse.builder()
                .id(history.getId())
                .courseKey(history.getCourseKey())
                .capacity(history.getCapacity())
                .current(history.getCurrent())
                .detectedAt(history.getCreatedAt())
                .build();
    }
}
