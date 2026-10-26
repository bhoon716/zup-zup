package bhoon.sugang_helper.seatalert.notification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.util.EmailTemplateService;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.mail.internet.MimeMessage;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    private EmailNotificationSender emailNotificationSender;

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private EmailTemplateService templateService;

    @BeforeEach
    void setUp() {
        emailNotificationSender = new EmailNotificationSender(mailSender, templateService, "test@example.com",
                "TestSender");
    }

    @Test
    @DisplayName("이메일 발송 성공")
    void send_success() {

        String to = "recipient@example.com";
        String title = "제목";
        String content = "내용";
        given(mailSender.createMimeMessage()).willReturn(mock(MimeMessage.class));
        given(templateService.loadTemplate(eq("notification"), anyMap())).willReturn("<html>HTML Content</html>");


        emailNotificationSender.send(NotificationTarget.of(to), title, content);


        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(templateService, times(1)).loadTemplate(eq("notification"), anyMap());
    }

    @Test
    @DisplayName("이메일 채널 지원 여부 확인")
    void supports_email_channel() {
        assertThat(emailNotificationSender.supports(NotificationChannel.EMAIL)).isTrue();
        assertThat(emailNotificationSender.supports(NotificationChannel.FCM)).isFalse();
    }

    @Test
    @DisplayName("이메일 발송 실패 로그와 예외에 수신자 원문을 남기지 않는다")
    void send_failureDoesNotLogOrExposeRecipient() {
        String recipient = "recipient@example.com";
        given(mailSender.createMimeMessage()).willReturn(mock(MimeMessage.class));
        given(templateService.loadTemplate(eq("notification"), anyMap())).willReturn("<html>HTML Content</html>");
        org.mockito.Mockito.doThrow(new RuntimeException("provider rejected " + recipient))
                .when(mailSender).send(any(MimeMessage.class));
        Logger logger = (Logger) LoggerFactory.getLogger(EmailNotificationSender.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);

        try {
            assertThatThrownBy(() -> emailNotificationSender.send(NotificationTarget.of(recipient), "제목", "내용"))
                    .hasMessageNotContaining(recipient);
            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(messages).doesNotContain(recipient, "@example.com", "provider rejected")
                    .contains("recipientFingerprint=sha256:", "failureCode=N001", "exceptionType=RuntimeException");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previousLevel);
        }
    }
}
