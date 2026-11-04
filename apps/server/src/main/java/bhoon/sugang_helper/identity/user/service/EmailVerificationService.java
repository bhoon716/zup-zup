package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.common.util.EmailTemplateService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
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

    private static final String CODE_PREFIX = "EMAIL_CODE:";
    private static final String VERIFIED_PREFIX = "EMAIL_VERIFIED:";
    private static final String SEND_COOLDOWN_PREFIX = "EMAIL_SEND_COOLDOWN:";
    private static final String ATTEMPT_PREFIX = "EMAIL_CODE_ATTEMPTS:";
    private static final long CODE_EXPIRATION_MINUTES = 5;
    private static final long VERIFIED_EXPIRATION_MINUTES = 60;
    private static final Duration SEND_COOLDOWN = Duration.ofMinutes(1);
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final String EMAIL_SUBJECT = "[수강신청 도우미] 이메일 인증 코드";
    private final JavaMailSender javaMailSender;
    private final RedisService redisService;
    private final EmailTemplateService templateService;
    @Value("${spring.mail.from}")
    private String from;
    @Value("${spring.mail.from-name}")
    private String fromName;




    public void sendCode(Long userId, String email) {
        sendCode(userId, email, null);
    }

    public void sendCode(Long userId, String email, String clientIp) {
        SendCooldownLease cooldownLease = acquireSendCooldown(userId, email, clientIp);
        if (cooldownLease == null) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS, "인증 코드는 잠시 후 다시 요청할 수 있습니다.");
        }

        MimeMessage mimeMessage;
        try {
            String code = generateCode();
            String key = getCodeKey(userId, email);

            redisService.setValues(key, code, Duration.ofMinutes(CODE_EXPIRATION_MINUTES));

            String htmlContent = templateService.loadTemplate("verification_code", Map.of("code", code));
            mimeMessage = createEmailMessage(email, EMAIL_SUBJECT, htmlContent);
        } catch (RuntimeException exception) {
            releaseSendCooldown(cooldownLease);
            throw exception;
        }

        sendEmail(email, mimeMessage);
        log.info("[EmailVerification] 인증 코드 발송 완료. userId={}, emailFingerprint={}", userId,
                SensitiveDataRedactor.fingerprint(email));
    }




    public boolean verifyCode(Long userId, String email, String code) {
        String key = getCodeKey(userId, email);
        String savedCode = redisService.getValues(key);

        if (savedCode == null) {
            return false;
        }

        if (savedCode.equals(code)) {
            redisService.deleteValues(key);
            redisService.deleteValues(getAttemptKey(userId, email));
            redisService.setValues(getVerifiedKey(userId, email), "true",
                    Duration.ofMinutes(VERIFIED_EXPIRATION_MINUTES));
            log.info("[EmailVerification] 이메일 인증 완료. userId={}, emailFingerprint={}", userId,
                    SensitiveDataRedactor.fingerprint(email));
            return true;
        }

        long attempts = redisService.increment(getAttemptKey(userId, email), Duration.ofMinutes(CODE_EXPIRATION_MINUTES));
        if (attempts >= MAX_VERIFY_ATTEMPTS) {
            redisService.deleteValues(key);
            redisService.deleteValues(getAttemptKey(userId, email));
        }
        return false;
    }




    public boolean isVerified(Long userId, String email) {
        String value = redisService.getValues(getVerifiedKey(userId, email));
        return "true".equals(value);
    }




    public void clearVerificationState(Long userId, String... emails) {
        redisService.deleteValues(SEND_COOLDOWN_PREFIX + "USER:" + userId);
        if (emails == null) {
            return;
        }

        for (String email : emails) {
            if (email == null || email.isBlank()) {
                continue;
            }
            redisService.deleteValues(getCodeKey(userId, email));
            redisService.deleteValues(getVerifiedKey(userId, email));
            redisService.deleteValues(getAttemptKey(userId, email));
            redisService.deleteValues(SEND_COOLDOWN_PREFIX + "EMAIL:" + email);
        }
    }




    private MimeMessage createEmailMessage(String to, String title, String content) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(new InternetAddress(from, fromName, "UTF-8"));
            helper.setTo(to);
            helper.setSubject(title);
            helper.setText(content, true);
            return mimeMessage;
        } catch (Exception e) {
            throw emailSendException(to, e);
        }
    }

    private void sendEmail(String to, MimeMessage mimeMessage) {
        try {
            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            throw emailSendException(to, e);
        }
    }

    private CustomException emailSendException(String to, Exception exception) {
        log.error("[EmailVerification] 인증 이메일 발송 실패. recipientFingerprint={}, failureCode={}, exceptionType={}",
                SensitiveDataRedactor.fingerprint(to), ErrorCode.EMAIL_SEND_ERROR.getCode(),
                SensitiveDataRedactor.exceptionType(exception));
        return new CustomException(ErrorCode.EMAIL_SEND_ERROR, "Failed to send verification email");
    }




    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private String getCodeKey(Long userId, String email) {
        return CODE_PREFIX + userId + ":" + email;
    }

    private String getVerifiedKey(Long userId, String email) {
        return VERIFIED_PREFIX + userId + ":" + email;
    }

    private SendCooldownLease acquireSendCooldown(Long userId, String email, String clientIp) {
        String token = UUID.randomUUID().toString();
        String userKey = SEND_COOLDOWN_PREFIX + "USER:" + userId;
        String emailKey = SEND_COOLDOWN_PREFIX + "EMAIL:" + email;
        String ipKey = clientIp == null || clientIp.isBlank()
                ? null : SEND_COOLDOWN_PREFIX + "IP:" + clientIp;
        boolean userAcquired = false;
        boolean emailAcquired = false;
        try {
            userAcquired = redisService.setValuesIfAbsent(userKey, token, SEND_COOLDOWN);
            if (!userAcquired) {
                return null;
            }

            emailAcquired = redisService.setValuesIfAbsent(emailKey, token, SEND_COOLDOWN);
            if (!emailAcquired) {
                releaseCooldownKeys(token, userKey);
                return null;
            }

            if (ipKey == null || redisService.setValuesIfAbsent(ipKey, token, SEND_COOLDOWN)) {
                return new SendCooldownLease(token, userKey, emailKey, ipKey);
            }

            releaseCooldownKeys(token, userKey, emailKey);
            return null;
        } catch (RuntimeException exception) {
            if (emailAcquired) {
                releaseCooldownKeys(token, userKey, emailKey);
            } else if (userAcquired) {
                releaseCooldownKeys(token, userKey);
            }
            throw exception;
        }
    }

    private void releaseSendCooldown(SendCooldownLease lease) {
        releaseCooldownKeys(lease.token(), lease.userKey(), lease.emailKey(), lease.ipKey());
    }

    private void releaseCooldownKeys(String token, String... keys) {
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            try {
                redisService.compareAndDeleteValues(key, token);
            } catch (RuntimeException exception) {
                log.warn("[EmailVerification] 발송 실패 후 대기 제한 복구 실패. exceptionType={}",
                        SensitiveDataRedactor.exceptionType(exception));
            }
        }
    }

    private String getAttemptKey(Long userId, String email) {
        return ATTEMPT_PREFIX + userId + ":" + email;
    }

    private record SendCooldownLease(String token, String userKey, String emailKey, String ipKey) {
    }
}
