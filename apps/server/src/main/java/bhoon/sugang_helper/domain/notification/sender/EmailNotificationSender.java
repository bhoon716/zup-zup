package bhoon.sugang_helper.domain.notification.sender;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.EmailTemplateService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 이메일 채널을 통한 알림 발송을 담당하는 컴포넌트입니다.
 */
@Component
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender javaMailSender;
    private final EmailTemplateService templateService;
    private final String from;
    private final String fromName;

    public EmailNotificationSender(
            JavaMailSender javaMailSender,
            EmailTemplateService templateService,
            @Value("${spring.mail.from}") String from,
            @Value("${spring.mail.from-name}") String fromName) {
        this.javaMailSender = javaMailSender;
        this.templateService = templateService;
        this.from = from;
        this.fromName = fromName;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    /**
     * 알림 대상에게 이메일을 발송합니다.
     */
    @Override
    public void send(NotificationTarget target, String title, String message) {
        String recipient = target.getRecipient();
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String htmlContent = templateService.loadTemplate("notification", Map.of("message", message));

            helper.setFrom(new InternetAddress(from, fromName, "UTF-8"));
            helper.setTo(recipient);
            helper.setSubject(title);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            log.info("[EmailNotification] Dispatched - recipient={}, from={}", recipient, from);
        } catch (Exception e) {
            log.error("[EmailNotification] Failed to send email to {}", recipient, e);
            throw new CustomException(ErrorCode.EMAIL_SEND_ERROR, "Recipient: " + recipient);
        }
    }
}
