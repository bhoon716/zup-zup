package bhoon.sugang_helper.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.util.EmailTemplateService;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final String FROM = "from@example.com";
    private static final String FROM_NAME = "SugangHelper";
    private static final String CODE = "123456";

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private RedisService redisService;

    @Mock
    private EmailTemplateService templateService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("인증 코드를 생성하여 Redis에 저장하고 이메일을 발송한다")
    void sendCode() {
        // given
        Long userId = 1L;
        ReflectionTestUtils.setField(emailVerificationService, "from", FROM);
        ReflectionTestUtils.setField(emailVerificationService, "fromName", FROM_NAME);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.loadTemplate(eq("verification_code"), anyMap())).thenReturn("<html>HTML</html>");
        when(redisService.setValuesIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        // when
        emailVerificationService.sendCode(userId, EMAIL);

        // then
        verify(redisService, times(1)).setValues(eq("EMAIL_CODE:" + userId + ":" + EMAIL), anyString(),
                any(Duration.class));
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("이메일 발송 실패 시 예외를 발생시킨다")
    void sendCode_MailSendError_ThrowsException() {
        // given
        Long userId = 1L;
        ReflectionTestUtils.setField(emailVerificationService, "from", FROM);
        ReflectionTestUtils.setField(emailVerificationService, "fromName", FROM_NAME);

        when(javaMailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        when(templateService.loadTemplate(eq("verification_code"), anyMap())).thenReturn("<html>HTML</html>");
        when(redisService.setValuesIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        doThrow(new RuntimeException("Mail server error")).when(javaMailSender).send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendCode(userId, EMAIL))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_SEND_ERROR);
    }

    @Test
    void sendCode_RejectsCooldown() {
        when(redisService.setValuesIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> emailVerificationService.sendCode(1L, EMAIL))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("올바른 코드로 인증을 완료한다")
    void verifyCode_Success() {
        // given
        Long userId = 1L;
        String email = EMAIL;
        String code = CODE;
        String key = "EMAIL_CODE:" + userId + ":" + email;

        when(redisService.getValues(key)).thenReturn(code);

        // when
        boolean result = emailVerificationService.verifyCode(userId, email, code);

        // then
        assertThat(result).isTrue();
        verify(redisService).deleteValues(key);
        verify(redisService).setValues(eq("EMAIL_VERIFIED:" + userId + ":" + email), eq("true"), any(Duration.class));
    }

    @Test
    @DisplayName("잘못된 코드로 인증 시도 시 실패한다")
    void verifyCode_WrongCode_ReturnsFalse() {
        // given
        Long userId = 1L;
        String email = EMAIL;
        String key = "EMAIL_CODE:" + userId + ":" + email;

        when(redisService.getValues(key)).thenReturn("123456");

        // when
        boolean result = emailVerificationService.verifyCode(userId, email, "654321");

        // then
        assertThat(result).isFalse();
        verify(redisService, never()).deleteValues(anyString());
    }

    @Test
    void verifyCode_DeletesCodeAfterMaximumFailedAttempts() {
        Long userId = 1L;
        String key = "EMAIL_CODE:" + userId + ":" + EMAIL;
        when(redisService.getValues(key)).thenReturn(CODE);
        when(redisService.increment(anyString(), any(Duration.class))).thenReturn(5L);

        assertThat(emailVerificationService.verifyCode(userId, EMAIL, "000000")).isFalse();
        verify(redisService).deleteValues(key);
        verify(redisService).deleteValues("EMAIL_CODE_ATTEMPTS:" + userId + ":" + EMAIL);
    }

    @Test
    @DisplayName("인증 여부를 확인한다")
    void isVerified() {
        // given
        Long userId = 1L;
        String email = EMAIL;
        String key = "EMAIL_VERIFIED:" + userId + ":" + email;

        when(redisService.getValues(key)).thenReturn("true");

        // when
        boolean result = emailVerificationService.isVerified(userId, email);

        // then
        assertThat(result).isTrue();
    }
}
