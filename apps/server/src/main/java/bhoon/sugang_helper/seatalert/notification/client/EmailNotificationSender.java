package bhoon.sugang_helper.seatalert.notification.client;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import bhoon.sugang_helper.common.api.templates.EmailTemplates;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

@Component
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private static final String EVENT_KEY = "event";
    private static final String RECIPIENT_FINGERPRINT_KEY = "recipientFingerprint";

    private final JavaMailSender javaMailSender;
    private final EmailTemplates templateService;
    private final String from;
    private final String fromName;

    public EmailNotificationSender(
            JavaMailSender javaMailSender,
            EmailTemplates templateService,
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
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            String htmlContent = templateService.loadTemplate("notification", Map.of("message", HtmlUtils.htmlEscape(message)));

            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(recipient);
            helper.setSubject(title);
            helper.setText(htmlContent, true);
            if (StringUtils.hasText(idempotencyKey)) {
                mimeMessage.setHeader("X-Idempotency-Key", idempotencyKey);
            }

            javaMailSender.send(mimeMessage);
            logEmailSent(recipientFingerprint, SensitiveDataRedactor.fingerprint(from));
        } catch (Exception e) {
            logEmailFailed(recipientFingerprint, ErrorCode.EMAIL_SEND_ERROR.getCode(), SensitiveDataRedactor.exceptionType(e));
            throw new CustomException(ErrorCode.EMAIL_SEND_ERROR);
        }
    }

    private void logEmailSent(String recipientFingerprint, String fromFingerprint) {
        log.atDebug().addKeyValue(EVENT_KEY, "email_notification_sent")
                .addKeyValue(RECIPIENT_FINGERPRINT_KEY, recipientFingerprint)
                .addKeyValue("fromFingerprint", fromFingerprint)
                .log("[EmailNotification] 이메일 발송 완료.");
    }

    private void logEmailFailed(String recipientFingerprint, String failureCode, String exceptionType) {
        log.atDebug().addKeyValue(EVENT_KEY, "email_notification_failed")
                .addKeyValue(RECIPIENT_FINGERPRINT_KEY, recipientFingerprint)
                .addKeyValue("failureCode", failureCode)
                .addKeyValue("exceptionType", exceptionType)
                .log("[EmailNotification] 이메일 발송 실패.");
    }

}
