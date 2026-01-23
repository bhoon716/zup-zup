package bhoon.sugang_helper.domain.notification.sender;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender javaMailSender;
    private final String from;
    private final String fromName;

    public EmailNotificationSender(
            JavaMailSender javaMailSender,
            @Value("${spring.mail.from}") String from,
            @Value("${spring.mail.from-name}") String fromName) {
        this.javaMailSender = javaMailSender;
        this.from = from;
        this.fromName = fromName;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationTarget target, String title, String message) {
        String recipient = target.getRecipient();
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(new InternetAddress(from, fromName, "UTF-8"));
            helper.setTo(recipient);
            helper.setSubject(title);
            helper.setText(message);

            javaMailSender.send(mimeMessage);
            log.info("[Email] Sent to: {} from: {} ({})", recipient, from, fromName);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.EMAIL_SEND_ERROR, "Recipient: " + recipient);
        }
    }
}
