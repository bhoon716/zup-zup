package bhoon.sugang_helper.seatalert.notification.client;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.common.util.EmailTemplateService;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;




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




    @Override
    public void send(NotificationTarget target, String title, String message) {
        send(target, title, message, null);
    }

    @Override
    public void send(NotificationTarget target, String title, String message, String idempotencyKey) {
        String recipient = target.getRecipient();
        String recipientFingerprint = SensitiveDataRedactor.fingerprint(recipient);
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String htmlContent = templateService.loadTemplate("notification", Map.of("message", message));

            helper.setFrom(new InternetAddress(from, fromName, "UTF-8"));
            helper.setTo(recipient);
            helper.setSubject(title);
            helper.setText(htmlContent, true);
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                mimeMessage.setHeader("X-Idempotency-Key", idempotencyKey);
            }

            javaMailSender.send(mimeMessage);
            log.debug("[EmailNotification] 이메일 발송 완료. recipientFingerprint={}, fromFingerprint={}",
                    recipientFingerprint, SensitiveDataRedactor.fingerprint(from));
        } catch (Exception e) {
            log.debug("[EmailNotification] 이메일 발송 실패. recipientFingerprint={}, failureCode={}, exceptionType={}",
                    recipientFingerprint, ErrorCode.EMAIL_SEND_ERROR.getCode(), SensitiveDataRedactor.exceptionType(e));
            throw new CustomException(ErrorCode.EMAIL_SEND_ERROR);
        }
    }
}
