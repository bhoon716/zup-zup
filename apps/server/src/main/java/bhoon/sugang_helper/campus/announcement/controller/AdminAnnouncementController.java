package bhoon.sugang_helper.campus.announcement.controller;

import bhoon.sugang_helper.common.web.PageableGuard;
import bhoon.sugang_helper.campus.announcement.dto.request.AnnouncementRequest;
import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementDetailResponse;
import bhoon.sugang_helper.campus.announcement.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 관리자용 전체 공지사항 목록을 조회합니다. (비공개 포함)
     */
    @GetMapping
    public ResponseEntity<Page<AnnouncementDetailResponse>> getAnnouncements(
            Pageable pageable) {
        Pageable fixedPageable = PageableGuard.fixedSize(pageable);
        Page<AnnouncementDetailResponse> response = announcementService.getAdminAnnouncements(fixedPageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 새로운 공지사항을 작성하고 등록합니다.
     */
    @PostMapping
    public ResponseEntity<AnnouncementDetailResponse> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request) {
        AnnouncementDetailResponse response = announcementService.createAnnouncement(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 기존 공지사항 정보를 수정합니다.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementDetailResponse> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequest request) {
        AnnouncementDetailResponse response = announcementService.updateAnnouncement(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 공지사항을 삭제합니다.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok(null);
    }
}
