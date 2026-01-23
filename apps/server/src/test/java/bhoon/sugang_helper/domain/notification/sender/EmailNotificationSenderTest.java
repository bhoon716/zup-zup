package bhoon.sugang_helper.domain.notification.sender;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    private EmailNotificationSender emailNotificationSender;

    @Mock
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        emailNotificationSender = new EmailNotificationSender(mailSender, "test@example.com", "TestSender");
    }

    @Test
    @DisplayName("이메일 발송 성공")
    void send_success() {
        // given
        String to = "recipient@example.com";
        String title = "제목";
        String content = "내용";
        given(mailSender.createMimeMessage()).willReturn(mock(MimeMessage.class));

        // when
        emailNotificationSender.send(to, title, content);

        // then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("이메일 채널 지원 여부 확인")
    void supports_email_channel() {
        assertThat(emailNotificationSender.supports(NotificationChannel.EMAIL)).isTrue();
        assertThat(emailNotificationSender.supports(NotificationChannel.FCM)).isFalse();
    }
}
