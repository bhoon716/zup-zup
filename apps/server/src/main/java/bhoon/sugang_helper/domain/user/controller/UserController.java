package bhoon.sugang_helper.domain.user.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.user.response.UserResponse;
import bhoon.sugang_helper.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<CommonResponse<UserResponse>> getMyProfile() {
        UserResponse response = userService.getMyProfile();
        return CommonResponse.ok(response, "사용자 프로필 정보입니다.");
    }

    @DeleteMapping("/me")
    public ResponseEntity<CommonResponse<Void>> withdraw() {
        userService.withdraw();
        return CommonResponse.ok(null, "회원 탈퇴가 완료되었습니다.");
    }
}
