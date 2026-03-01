package bhoon.sugang_helper.domain.announcement.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.announcement.request.AnnouncementSearchType;
import bhoon.sugang_helper.domain.announcement.response.AnnouncementDetailResponse;
import bhoon.sugang_helper.domain.announcement.response.AnnouncementListResponse;
import bhoon.sugang_helper.domain.announcement.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements", description = "공지사항 조회 API")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 공개된 공지사항 목록을 검색 조건에 따라 조회합니다.
     */
    @Operation(summary = "공지사항 목록 조회", description = "공개된 공지사항 목록을 조회합니다. 고정 공지가 먼저 노출됩니다.")
    @GetMapping
    public ResponseEntity<CommonResponse<List<AnnouncementListResponse>>> getAnnouncements(
            @Parameter(description = "검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "검색 타입 (TITLE, CONTENT, TITLE_CONTENT)") @RequestParam(defaultValue = "TITLE_CONTENT") AnnouncementSearchType searchType) {
        List<AnnouncementListResponse> response = announcementService.getPublicAnnouncements(keyword, searchType);
        return CommonResponse.ok(response, "공지사항 목록입니다.");
    }

    /**
     * 특정 공지사항의 상세 내용을 조회합니다.
     */
    @Operation(summary = "공지사항 상세 조회", description = "공개된 공지사항 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<AnnouncementDetailResponse>> getAnnouncement(@PathVariable Long id) {
        AnnouncementDetailResponse response = announcementService.getPublicAnnouncement(id);
        return CommonResponse.ok(response, "공지사항 상세입니다.");
    }
}
