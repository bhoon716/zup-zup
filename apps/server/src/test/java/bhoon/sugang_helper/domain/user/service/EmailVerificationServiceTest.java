package bhoon.sugang_helper.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("인증 코드를 생성하여 Redis에 저장하고 이메일을 발송한다")
    void sendCode() {
        // given
        Long userId = 1L;
        String email = "test@example.com";
        ReflectionTestUtils.setField(emailVerificationService, "from", "from@example.com");
        ReflectionTestUtils.setField(emailVerificationService, "fromName", "SugangHelper");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        emailVerificationService.sendCode(userId, email);

        // then
        verify(redisService, times(1)).setValues(eq("EMAIL_CODE:" + userId + ":" + email), anyString(),
                any(Duration.class));
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("이메일 발송 실패 시 예외를 발생시킨다")
    void sendCode_MailSendError_ThrowsException() {
        // given
        Long userId = 1L;
        String email = "test@example.com";
        ReflectionTestUtils.setField(emailVerificationService, "from", "from@example.com");
        ReflectionTestUtils.setField(emailVerificationService, "fromName", "SugangHelper");

        when(javaMailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        doThrow(new RuntimeException("Mail server error")).when(javaMailSender).send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendCode(userId, email))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_SEND_ERROR);
    }

    @Test
    @DisplayName("올바른 코드로 인증을 완료한다")
    void verifyCode_Success() {
        // given
        Long userId = 1L;
        String email = "test@example.com";
        String code = "123456";
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
        String email = "test@example.com";
        String key = "EMAIL_CODE:" + userId + ":" + email;

        when(redisService.getValues(key)).thenReturn("123456");

        // when
        boolean result = emailVerificationService.verifyCode(userId, email, "654321");

        // then
        assertThat(result).isFalse();
        verify(redisService, never()).deleteValues(anyString());
    }

    @Test
    @DisplayName("인증 여부를 확인한다")
    void isVerified() {
        // given
        Long userId = 1L;
        String email = "test@example.com";
        String key = "EMAIL_VERIFIED:" + userId + ":" + email;

        when(redisService.getValues(key)).thenReturn("true");

        // when
        boolean result = emailVerificationService.isVerified(userId, email);

        // then
        assertThat(result).isTrue();
    }
}
