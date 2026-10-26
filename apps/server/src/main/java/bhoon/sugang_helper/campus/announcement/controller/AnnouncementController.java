package bhoon.sugang_helper.campus.announcement.controller;

import bhoon.sugang_helper.common.web.PageableGuard;
import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementDetailResponse;
import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementListResponse;
import bhoon.sugang_helper.campus.announcement.enums.AnnouncementSearchType;
import bhoon.sugang_helper.campus.announcement.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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




    @GetMapping
    public ResponseEntity<Page<AnnouncementListResponse>> getAnnouncements(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "TITLE_CONTENT") AnnouncementSearchType searchType,
            Pageable pageable) {
        Pageable fixedPageable = PageableGuard.fixedSize(pageable);
        Page<AnnouncementListResponse> response = announcementService.getPublicAnnouncements(keyword, searchType, fixedPageable);
        return ResponseEntity.ok(response);
    }




    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementDetailResponse> getAnnouncement(@PathVariable Long id) {
        AnnouncementDetailResponse response = announcementService.getPublicAnnouncement(id);
        return ResponseEntity.ok(response);
    }
}
