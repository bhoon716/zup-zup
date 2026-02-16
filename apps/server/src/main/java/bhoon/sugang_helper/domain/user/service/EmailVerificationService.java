package bhoon.sugang_helper.domain.user.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final JavaMailSender javaMailSender;
    private final RedisService redisService;

    @Value("${spring.mail.from}")
    private String from;

    @Value("${spring.mail.from-name}")
    private String fromName;

    private static final String CODE_PREFIX = "EMAIL_CODE:";
    private static final String VERIFIED_PREFIX = "EMAIL_VERIFIED:";
    private static final long CODE_EXPIRATION_MINUTES = 5;
    private static final long VERIFIED_EXPIRATION_MINUTES = 60;

    public void sendCode(Long userId, String email) {
        String code = generateCode();
        String key = getCodeKey(userId, email);

        redisService.setValues(key, code, Duration.ofMinutes(CODE_EXPIRATION_MINUTES));
        sendEmail(email, "[수강신청 도우미] 이메일 인증 코드", "인증 코드: " + code);

        log.info("[EmailVerification] Sent code to user={}, email={}", userId, email);
    }

    public boolean verifyCode(Long userId, String email, String code) {
        String key = getCodeKey(userId, email);
        String savedCode = redisService.getValues(key);

        if (savedCode == null) {
            return false;
        }

        if (savedCode.equals(code)) {
            redisService.deleteValues(key);
            redisService.setValues(getVerifiedKey(userId, email), "true",
                    Duration.ofMinutes(VERIFIED_EXPIRATION_MINUTES));
            log.info("[EmailVerification] Verified user={}, email={}", userId, email);
            return true;
        }

        return false;
    }

    public boolean isVerified(Long userId, String email) {
        String value = redisService.getValues(getVerifiedKey(userId, email));
        return "true".equals(value);
    }

    private void sendEmail(String to, String title, String content) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(new InternetAddress(from, fromName, "UTF-8"));
            helper.setTo(to);
            helper.setSubject(title);
            helper.setText(content);

            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("[EmailVerification] Failed to send email to {}", to, e);
            throw new CustomException(ErrorCode.EMAIL_SEND_ERROR, "Failed to send verification email");
        }
    }

    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private String getCodeKey(Long userId, String email) {
        return CODE_PREFIX + userId + ":" + email;
    }

    private String getVerifiedKey(Long userId, String email) {
        return VERIFIED_PREFIX + userId + ":" + email;
    }
}
