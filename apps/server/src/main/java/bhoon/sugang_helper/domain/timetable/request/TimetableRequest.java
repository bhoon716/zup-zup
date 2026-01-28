package bhoon.sugang_helper.domain.timetable.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TimetableRequest {

    @NotBlank(message = "시간표 이름을 입력해주세요.")
    @Size(max = 50, message = "시간표 이름은 50자 이내여야 합니다.")
    private String name;

    private boolean isPrimary;

    public TimetableRequest(String name, boolean isPrimary) {
        this.name = name;
        this.isPrimary = isPrimary;
    }
}
