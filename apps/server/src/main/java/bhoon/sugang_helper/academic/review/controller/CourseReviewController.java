package bhoon.sugang_helper.academic.review.controller;

import bhoon.sugang_helper.academic.review.dto.request.ReviewCreateRequest;
import bhoon.sugang_helper.academic.review.dto.request.ReviewReactionRequest;
import bhoon.sugang_helper.academic.review.dto.request.ReviewUpdateRequest;
import bhoon.sugang_helper.academic.review.dto.response.ReviewResponse;
import bhoon.sugang_helper.academic.review.service.CourseReviewService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.common.web.PageableGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseReviewController {

    private final CourseReviewService reviewService;

    @PostMapping("/{courseKey}/reviews")
    public ResponseEntity<CommonResponse<ReviewResponse>> createReview(
            @PathVariable String courseKey,
            @Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse response = reviewService.createReview(courseKey, request);
        return CommonResponse.ok(response, "리뷰가 성공적으로 작성되었습니다.");
    }

    @GetMapping("/{courseKey}/reviews")
    public ResponseEntity<CommonResponse<Page<ReviewResponse>>> getReviews(
            @PathVariable String courseKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        validatePagination(page, size);
        if (sort == null || sort.length == 0 || sort[0].isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "정렬 기준은 비어 있을 수 없습니다.");
        }
        String sortProperty = sort[0];
        Sort.Direction direction;
        try {
            direction = sort.length > 1 ? Sort.Direction.fromString(sort[1]) : Sort.Direction.DESC;
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "정렬 방향은 asc 또는 desc만 지원합니다.");
        }
        Pageable pageRequest = PageableGuard.requireBounded(
                PageRequest.of(page, size, Sort.by(direction, sortProperty)));

        Page<ReviewResponse> responsePage = reviewService.getReviews(courseKey, pageRequest);
        return CommonResponse.ok(responsePage, "리뷰 목록을 조회했습니다.");
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "페이지 번호는 0 이상, 크기는 1 이상이어야 합니다.");
        }
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<CommonResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request) {
        ReviewResponse response = reviewService.updateReview(reviewId, request);
        return CommonResponse.ok(response, "리뷰가 수정되었습니다.");
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<CommonResponse<Void>> deleteReview(
            @PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return CommonResponse.ok(null, "리뷰가 삭제되었습니다.");
    }

    @PostMapping("/reviews/{reviewId}/reaction")
    public ResponseEntity<CommonResponse<Void>> toggleReaction(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReactionRequest request) {
        reviewService.toggleReaction(reviewId, request);
        return CommonResponse.ok(null, "반응 처리가 완료되었습니다.");
    }
}
