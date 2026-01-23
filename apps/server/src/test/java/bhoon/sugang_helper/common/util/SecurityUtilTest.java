package bhoon.sugang_helper.common.util;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityUtilTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("현재 사용자 이메일 추출 성공")
    void getCurrentUserEmail_success() {
        // given
        String email = "test@example.com";
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(email);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when
        String result = SecurityUtil.getCurrentUserEmail();

        // then
        assertThat(result).isEqualTo(email);
    }

    @Test
    @DisplayName("인증 정보가 없을 때 예외 발생")
    void getCurrentUserEmail_no_authentication() {
        // given
        SecurityContextHolder.clearContext();

        // when & then
        assertThatThrownBy(SecurityUtil::getCurrentUserEmail)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }
}
