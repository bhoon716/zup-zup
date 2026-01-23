package bhoon.sugang_helper.domain.subscription.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.subscription.request.SubscriptionRequest;
import bhoon.sugang_helper.domain.subscription.response.SubscriptionResponse;
import bhoon.sugang_helper.domain.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<CommonResponse<SubscriptionResponse>> subscribe(
            @RequestBody @Valid SubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.subscribe(request);
        return CommonResponse.ok(response, "구독 신청이 완료되었습니다.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> unsubscribe(@PathVariable Long id) {
        subscriptionService.unsubscribe(id);
        return CommonResponse.ok(null, "구독이 취소되었습니다.");
    }

    @GetMapping
    public ResponseEntity<CommonResponse<List<SubscriptionResponse>>> getMySubscriptions() {
        List<SubscriptionResponse> responses = subscriptionService.getMySubscriptions();
        return CommonResponse.ok(responses, "내 구독 목록입니다.");
    }
}
