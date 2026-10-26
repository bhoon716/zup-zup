package bhoon.sugang_helper.planning.timetable.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TimetableCourseRequest {

    @NotBlank(message = "강좌 키를 입력해주세요.")
    private String courseKey;
}
