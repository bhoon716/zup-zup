package bhoon.sugang_helper.campus.announcement.controller;

import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementDetailResponse;
import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementListResponse;
import bhoon.sugang_helper.campus.announcement.enums.AnnouncementSearchType;
import bhoon.sugang_helper.campus.announcement.service.AnnouncementService;
import bhoon.sugang_helper.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 공개된 공지사항 목록을 검색 조건에 따라 조회합니다.
     */
    @GetMapping
    public ResponseEntity<CommonResponse<Page<AnnouncementListResponse>>> getAnnouncements(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "TITLE_CONTENT") AnnouncementSearchType searchType,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AnnouncementListResponse> response = announcementService.getPublicAnnouncements(keyword, searchType, pageable);
        return CommonResponse.ok(response, "공지사항 목록입니다.");
    }

    /**
     * 특정 공지사항의 상세 내용을 조회합니다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<AnnouncementDetailResponse>> getAnnouncement(@PathVariable Long id) {
        AnnouncementDetailResponse response = announcementService.getPublicAnnouncement(id);
        return CommonResponse.ok(response, "공지사항 상세입니다.");
    }
}
