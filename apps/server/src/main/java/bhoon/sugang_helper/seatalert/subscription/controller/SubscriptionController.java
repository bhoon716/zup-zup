package bhoon.sugang_helper.seatalert.subscription.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.seatalert.subscription.dto.request.SubscriptionRequest;
import bhoon.sugang_helper.seatalert.subscription.dto.response.SubscriptionResponse;
import bhoon.sugang_helper.seatalert.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @DeleteMapping
    public ResponseEntity<CommonResponse<Void>> unsubscribeAll() {
        subscriptionService.unsubscribeAll();
        return CommonResponse.ok(null, "모든 구독이 취소되었습니다.");
    }

    @GetMapping
    public ResponseEntity<CommonResponse<List<SubscriptionResponse>>> getMySubscriptions() {
        List<SubscriptionResponse> responses = subscriptionService.getMySubscriptions();
        return CommonResponse.ok(responses, "내 구독 목록입니다.");
    }
}
