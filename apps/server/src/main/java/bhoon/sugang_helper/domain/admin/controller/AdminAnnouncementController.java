package bhoon.sugang_helper.domain.admin.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.announcement.request.AnnouncementRequest;
import bhoon.sugang_helper.domain.announcement.response.AnnouncementDetailResponse;
import bhoon.sugang_helper.domain.announcement.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/announcements")
@RequiredArgsConstructor
@SecurityRequirement(name = "Cookie")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Announcements", description = "관리자 공지사항 관리 API")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 관리자용 전체 공지사항 목록을 조회합니다. (비공개 포함)
     */
    @Operation(summary = "공지사항 전체 조회", description = "공개/비공개를 포함한 전체 공지사항을 조회합니다.")
    @GetMapping
    public ResponseEntity<CommonResponse<List<AnnouncementDetailResponse>>> getAnnouncements() {
        List<AnnouncementDetailResponse> response = announcementService.getAdminAnnouncements();
        return CommonResponse.ok(response, "전체 공지사항 목록입니다.");
    }

    /**
     * 새로운 공지사항을 작성하고 등록합니다.
     */
    @Operation(summary = "공지사항 작성", description = "관리자 권한으로 공지사항을 작성합니다.")
    @PostMapping
    public ResponseEntity<CommonResponse<AnnouncementDetailResponse>> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request) {
        AnnouncementDetailResponse response = announcementService.createAnnouncement(request);
        return CommonResponse.ok(response, "공지사항을 등록했습니다.");
    }

    /**
     * 기존 공지사항 정보를 수정합니다.
     */
    @Operation(summary = "공지사항 수정", description = "관리자 권한으로 공지사항을 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<AnnouncementDetailResponse>> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequest request) {
        AnnouncementDetailResponse response = announcementService.updateAnnouncement(id, request);
        return CommonResponse.ok(response, "공지사항을 수정했습니다.");
    }

    /**
     * 특정 공지사항을 삭제합니다.
     */
    @Operation(summary = "공지사항 삭제", description = "관리자 권한으로 공지사항을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return CommonResponse.ok(null, "공지사항을 삭제했습니다.");
    }
}
