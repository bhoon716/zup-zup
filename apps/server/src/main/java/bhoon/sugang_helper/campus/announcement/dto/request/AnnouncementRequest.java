package bhoon.sugang_helper.campus.announcement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnnouncementRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private Boolean pinned;

    private Boolean published;

    public boolean isPinnedOrDefault() {
        return Boolean.TRUE.equals(pinned);
    }

    public boolean isPublishedOrDefault() {
        return published == null || published;
    }
}
