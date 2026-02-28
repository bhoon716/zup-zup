package bhoon.sugang_helper.domain.announcement.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "공지사항 등록/수정 요청")
public class AnnouncementRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하로 입력해주세요.")
    @Schema(description = "공지사항 제목", example = "[중요] 수강정정 일정 안내")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Schema(description = "공지사항 본문", example = "수강정정 기간은 3월 4일부터 3월 8일까지입니다.")
    private String content;

    @Schema(description = "상단 고정 여부", example = "true")
    private Boolean pinned;

    @Schema(description = "공개 여부", example = "true")
    private Boolean published;

    public boolean isPinnedOrDefault() {
        return Boolean.TRUE.equals(pinned);
    }

    public boolean isPublishedOrDefault() {
        return published == null || published;
    }
}
