package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.common.api.cache.RedisAccess;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import bhoon.sugang_helper.common.api.templates.EmailTemplates;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
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
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String EMAIL_COOLDOWN_KEY_SUFFIX = "EMAIL:";
    private static final String USER_COOLDOWN_KEY_SUFFIX = "USER:";
    private static final String EMAIL_FINGERPRINT_KEY = "emailFingerprint";
    private static final String EXCEPTION_TYPE_KEY = "exceptionType";
    private static final String VERIFIED_VALUE = "true";
    private static final String USER_ID_KEY = "userId";

    private static final String EVENT_KEY = "event";

    private static final String CODE_PREFIX = "EMAIL_CODE:";
    private static final String VERIFIED_PREFIX = "EMAIL_VERIFIED:";
    private static final String SEND_COOLDOWN_PREFIX = "EMAIL_SEND_COOLDOWN:";
    private static final String ATTEMPT_PREFIX = "EMAIL_CODE_ATTEMPTS:";
    private static final long CODE_EXPIRATION_MINUTES = 5;
    private static final long VERIFIED_EXPIRATION_MINUTES = 60;
    private static final Duration SEND_COOLDOWN = Duration.ofMinutes(1);
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int MIN_VERIFICATION_CODE = 100_000;
    private static final int VERIFICATION_CODE_UPPER_BOUND = 1_000_000;
    private static final String EMAIL_SUBJECT = "[수강신청 도우미] 이메일 인증 코드";
    private final JavaMailSender javaMailSender;
    private final RedisAccess redisService;
    private final EmailTemplates templateService;

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
        logEmailVerificationCodeSent(userId, SensitiveDataRedactor.fingerprint(email));
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
            redisService.setValues(getVerifiedKey(userId, email), VERIFIED_VALUE,
                    Duration.ofMinutes(VERIFIED_EXPIRATION_MINUTES));
            logEmailVerificationCompleted(userId, SensitiveDataRedactor.fingerprint(email));
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
        return VERIFIED_VALUE.equals(value);
    }

    public void clearVerificationState(Long userId, String... emails) {
        redisService.deleteValues(SEND_COOLDOWN_PREFIX + USER_COOLDOWN_KEY_SUFFIX + userId);
        if (emails == null) {
            return;
        }

        for (String email : emails) {
            if (!StringUtils.hasText(email)) {
                continue;
            }
            redisService.deleteValues(getCodeKey(userId, email));
            redisService.deleteValues(getVerifiedKey(userId, email));
            redisService.deleteValues(getAttemptKey(userId, email));
            redisService.deleteValues(SEND_COOLDOWN_PREFIX + EMAIL_COOLDOWN_KEY_SUFFIX + email);
        }
    }

    private MimeMessage createEmailMessage(String to, String title, String content) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
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
        logEmailVerificationSendFailed(SensitiveDataRedactor.fingerprint(to), ErrorCode.EMAIL_SEND_ERROR.getCode(), SensitiveDataRedactor.exceptionType(exception));
        return new CustomException(ErrorCode.EMAIL_SEND_ERROR, "인증 이메일을 발송하지 못했습니다.");
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = MIN_VERIFICATION_CODE + random.nextInt(VERIFICATION_CODE_UPPER_BOUND - MIN_VERIFICATION_CODE);
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
        String userKey = SEND_COOLDOWN_PREFIX + USER_COOLDOWN_KEY_SUFFIX + userId;
        String emailKey = SEND_COOLDOWN_PREFIX + EMAIL_COOLDOWN_KEY_SUFFIX + email;
        String ipKey = !StringUtils.hasText(clientIp)
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
                logEmailVerificationCooldownRestoreFailed(SensitiveDataRedactor.exceptionType(exception));
            }
        }
    }

    private String getAttemptKey(Long userId, String email) {
        return ATTEMPT_PREFIX + userId + ":" + email;
    }

    private void logEmailVerificationCodeSent(Long userId, String emailFingerprint) {
        log.atInfo().addKeyValue(EVENT_KEY, "email_verification_code_sent")
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue(EMAIL_FINGERPRINT_KEY, emailFingerprint)
                .log("[EmailVerification] 인증 코드 발송 완료.");
    }

    private void logEmailVerificationCompleted(Long userId, String emailFingerprint) {
        log.atInfo().addKeyValue(EVENT_KEY, "email_verification_completed")
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue(EMAIL_FINGERPRINT_KEY, emailFingerprint)
                .log("[EmailVerification] 이메일 인증 완료.");
    }

    private void logEmailVerificationSendFailed(String recipientFingerprint,
                                                String failureCode,
                                                String exceptionType) {
        log.atError().addKeyValue(EVENT_KEY, "email_verification_send_failed")
                .addKeyValue("recipientFingerprint", recipientFingerprint)
                .addKeyValue("failureCode", failureCode)
                .addKeyValue(EXCEPTION_TYPE_KEY, exceptionType)
                .log("[EmailVerification] 인증 이메일 발송 실패.");
    }

    private void logEmailVerificationCooldownRestoreFailed(String exceptionType) {
        log.atWarn().addKeyValue(EVENT_KEY, "email_verification_cooldown_restore_failed")
                .addKeyValue(EXCEPTION_TYPE_KEY, exceptionType)
                .log("[EmailVerification] 발송 실패 후 대기 제한 복구 실패.");
    }

    private record SendCooldownLease(String token, String userKey, String emailKey, String ipKey) {
    }
}
