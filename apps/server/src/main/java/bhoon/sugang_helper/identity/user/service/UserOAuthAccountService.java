package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.event.UserRegisteredEvent;
import bhoon.sugang_helper.identity.user.exception.OAuthAccountProvisioningException;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOAuthAccountService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthenticatedUser findOrCreate(String issuer, String subject, String email, String name) {
        User user = userRepository.findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(issuer, subject)
                .orElseGet(() -> bindLegacyOrCreateUser(issuer, subject, email, name));
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getRoleKey());
    }

    private User bindLegacyOrCreateUser(String issuer, String subject, String email, String name) {
        return userRepository.findByEmail(email)
                .map(user -> bindLegacyUser(user, issuer, subject))
                .orElseGet(() -> createUser(issuer, subject, email, name));
    }

    private User bindLegacyUser(User user, String issuer, String subject) {
        if (user.hasOAuthIdentity()) {
            throw new OAuthAccountProvisioningException(
                    "oauth_identity_conflict", "OAuth identity is already bound");
        }
        user.bindOAuthIdentity(issuer, subject);
        log.info("[OAuth2] Legacy user identity bound. userId={}", user.getId());
        return user;
    }

    private User createUser(String issuer, String subject, String email, String name) {
        User newUser = User.builder()
                .email(email)
                .name(name)
                .role(Role.USER)
                .oauthIssuer(issuer)
                .oauthSubject(subject)
                .build();
        User savedUser = userRepository.save(newUser);
        log.info("[OAuth2] New user sign-up complete. userId={}, emailMasked={}",
                savedUser.getId(), SensitiveDataRedactor.maskEmail(email));
        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getId(), email));
        return savedUser;
    }
}
