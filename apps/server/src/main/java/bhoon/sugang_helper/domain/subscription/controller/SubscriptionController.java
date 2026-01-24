package bhoon.sugang_helper.domain.subscription.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.subscription.request.SubscriptionRequest;
import bhoon.sugang_helper.domain.subscription.response.SubscriptionResponse;
import bhoon.sugang_helper.domain.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription", description = "구독 관련 API")
public class SubscriptionController {

  private final SubscriptionService subscriptionService;

  @Operation(summary = "강의 구독 신청", description = "특정 과목의 공석 알림을 위해 구독을 신청합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "구독 신청 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "구독 신청이 완료되었습니다.",
            "data": {
              "id": 1,
              "courseKey": "CLTR.0031-01",
              "subjectName": "기초프로그래밍",
              "isActive": true
            }
          }
          """)))
  })
  @PostMapping
  public ResponseEntity<CommonResponse<SubscriptionResponse>> subscribe(
      @RequestBody @Valid SubscriptionRequest request) {
    SubscriptionResponse response = subscriptionService.subscribe(request);
    return CommonResponse.ok(response, "구독 신청이 완료되었습니다.");
  }

  @Operation(summary = "강의 구독 취소", description = "특정 구독 이력을 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "구독 취소 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "구독이 취소되었습니다.",
            "data": null
          }
          """)))
  })
  @DeleteMapping("/{id}")
  public ResponseEntity<CommonResponse<Void>> unsubscribe(@PathVariable Long id) {
    subscriptionService.unsubscribe(id);
    return CommonResponse.ok(null, "구독이 취소되었습니다.");
  }

  @Operation(summary = "알림 활성화 토글", description = "특정 구독의 알림 활성화 상태를 변경합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "토글 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "알림 상태가 변경되었습니다.",
            "data": null
          }
          """)))
  })
  @PatchMapping("/{id}/toggle")
  public ResponseEntity<CommonResponse<Void>> toggleSubscription(@PathVariable Long id) {
    subscriptionService.toggleSubscription(id);
    return CommonResponse.ok(null, "알림 상태가 변경되었습니다.");
  }

  @Operation(summary = "내 구독 목록 조회", description = "현재 로그인한 사용자의 모든 구독 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "구독 목록 조회 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "내 구독 목록입니다.",
            "data": [
              {
                "id": 1,
                "courseKey": "CLTR.0031-01",
                "subjectName": "기초프로그래밍",
                "isActive": true
              }
            ]
          }
          """)))
  })
  @GetMapping
  public ResponseEntity<CommonResponse<List<SubscriptionResponse>>> getMySubscriptions() {
    List<SubscriptionResponse> responses = subscriptionService.getMySubscriptions();
    return CommonResponse.ok(responses, "내 구독 목록입니다.");
  }
}
