package bhoon.sugang_helper.domain.review.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.review.dto.request.ReviewCreateRequest;
import bhoon.sugang_helper.domain.review.dto.request.ReviewReactionRequest;
import bhoon.sugang_helper.domain.review.dto.request.ReviewUpdateRequest;
import bhoon.sugang_helper.domain.review.dto.response.ReviewResponse;
import bhoon.sugang_helper.domain.review.service.CourseReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course Review", description = "강의 리뷰 API (별점 리뷰 작성, 조회, 리액션)")
public class CourseReviewController {

  private final CourseReviewService reviewService;

  @Operation(summary = "강의 리뷰 작성", description = "선택한 과목에 1~5점의 별점과 익명 코멘트를 등록합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "작성 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "리뷰가 성공적으로 작성되었습니다.",
            "data": {
              "id": 1,
              "courseKey": "2026:U12345:0001:1",
              "rating": 5,
              "content": "이 강의 최고에요!",
              "likeCount": 0,
              "dislikeCount": 0,
              "isMine": true,
              "createdAt": "2026-03-08T17:00:00",
              "updatedAt": "2026-03-08T17:00:00"
            }
          }
          """))),
      @ApiResponse(responseCode = "400", description = "입력값 오류 (이미 작성했거나 별점 범위 이탈)", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
  })
  @PostMapping("/{courseKey}/reviews")
  public ResponseEntity<CommonResponse<ReviewResponse>> createReview(
      @PathVariable String courseKey,
      @Valid @RequestBody ReviewCreateRequest request) {
    ReviewResponse response = reviewService.createReview(courseKey, request);
    return CommonResponse.ok(response, "리뷰가 성공적으로 작성되었습니다.");
  }

  @Operation(summary = "해당 강의의 리뷰 목록 조회", description = "페이징 처리된 리뷰 목록을 반환합니다. (기본 최신순, 공감순 정렬 가능)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "리뷰 목록을 조회했습니다.",
            "data": {
              "content": [
                {
                  "id": 1,
                  "courseKey": "2026:U12345:0001:1",
                  "rating": 5,
                  "content": "정말 유익한 시간!",
                  "likeCount": 12,
                  "dislikeCount": 1,
                  "isMine": false,
                  "createdAt": "2026-03-08T12:00:00",
                  "updatedAt": "2026-03-08T12:00:00"
                }
              ],
              "pageable": "INSTANCE",
              "totalPages": 1,
              "totalElements": 1,
              "last": true,
              "size": 20,
              "number": 0,
              "sort": { "empty": false, "sorted": true, "unsorted": false },
              "first": true,
              "numberOfElements": 1,
              "empty": false
            }
          }
          """)))
  })
  @GetMapping("/{courseKey}/reviews")
  public ResponseEntity<CommonResponse<Page<ReviewResponse>>> getReviews(
      @PathVariable String courseKey,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
    String sortProperty = sort[0];
    Sort.Direction direction = sort.length > 1 ? Sort.Direction.fromString(sort[1]) : Sort.Direction.DESC;
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortProperty));

    Page<ReviewResponse> responsePage = reviewService.getReviews(courseKey, pageRequest);
    return CommonResponse.ok(responsePage, "리뷰 목록을 조회했습니다.");
  }

  @Operation(summary = "리뷰 수정 (백엔드 전용)", description = "자신이 작성한 리뷰 내용을 수정합니다. (현재 프론트엔드 연동 계획 없음)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
      @ApiResponse(responseCode = "403", description = "수정 권한 없음 (남의 글)", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
      @ApiResponse(responseCode = "404", description = "존재하지 않는 리뷰", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
  })
  @PutMapping("/reviews/{reviewId}")
  public ResponseEntity<CommonResponse<ReviewResponse>> updateReview(
      @PathVariable Long reviewId,
      @Valid @RequestBody ReviewUpdateRequest request) {
    ReviewResponse response = reviewService.updateReview(reviewId, request);
    return CommonResponse.ok(response, "리뷰가 수정되었습니다.");
  }

  @Operation(summary = "리뷰 삭제 (백엔드 전용)", description = "자신이 작성한 리뷰를 삭제합니다. (현재 프론트엔드 연동 계획 없음)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "삭제 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
      @ApiResponse(responseCode = "403", description = "삭제 권한 없음 (남의 글)", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
  })
  @DeleteMapping("/reviews/{reviewId}")
  public ResponseEntity<CommonResponse<Void>> deleteReview(
      @PathVariable Long reviewId) {
    reviewService.deleteReview(reviewId);
    return CommonResponse.ok(null, "리뷰가 삭제되었습니다.");
  }

  @Operation(summary = "리뷰 공감/비공감 토글", description = "특정 리뷰에 공감(LIKE) 또는 비공감(DISLIKE)을 표기합니다. 1인 1투표 제한 (재요청 시 취소 또는 변경)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "반응 처리 성공 (생성/변경/취소 자동 수행)", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "반응 처리가 완료되었습니다.",
            "data": null
          }
          """))),
      @ApiResponse(responseCode = "404", description = "존재하지 않는 리뷰", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
  })
  @PostMapping("/reviews/{reviewId}/reaction")
  public ResponseEntity<CommonResponse<Void>> toggleReaction(
      @PathVariable Long reviewId,
      @Valid @RequestBody ReviewReactionRequest request) {
    reviewService.toggleReaction(reviewId, request);
    return CommonResponse.ok(null, "반응 처리가 완료되었습니다.");
  }
}
