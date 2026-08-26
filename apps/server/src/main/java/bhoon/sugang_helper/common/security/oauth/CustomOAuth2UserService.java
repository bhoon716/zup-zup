package bhoon.sugang_helper.common.security.oauth;

import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRegisteredEvent;
import bhoon.sugang_helper.user.domain.UserRepository;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = getDelegateUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String issuer = providerIssuer(userRequest);
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = requiredAttribute(attributes, "email");
        String name = requiredAttribute(attributes, "name");
        String subject = requiredAttribute(attributes, userNameAttributeName);

        log.info("[OAuth2] Social login request. provider={}, emailMasked={}", registrationId,
                SensitiveDataRedactor.maskEmail(email));
        User user = findOrCreateUser(issuer, subject, email, name);
        log.info("Social login load complete (OAuth2): userId={}", user.getId());

        return new OAuth2AuthenticatedUser(
                user.getId(), user.getEmail(), user.getRoleKey(), attributes, userNameAttributeName);
    }

    private User findOrCreateUser(String issuer, String subject, String email, String name) {
        return userRepository.findByOauthIssuerAndOauthSubjectAndDeletedAtIsNull(issuer, subject)
                .orElseGet(() -> bindLegacyOrCreateUser(issuer, subject, email, name));
    }

    private User bindLegacyOrCreateUser(String issuer, String subject, String email, String name) {
        return userRepository.findByEmail(email)
                .map(user -> bindLegacyUser(user, issuer, subject))
                .orElseGet(() -> createUser(issuer, subject, email, name));
    }

    private User bindLegacyUser(User user, String issuer, String subject) {
        if (user.hasOAuthIdentity()) {
            throw authenticationFailure("oauth_identity_conflict", "OAuth identity is already bound");
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

    private String providerIssuer(OAuth2UserRequest userRequest) {
        String issuer = userRequest.getClientRegistration().getProviderDetails().getIssuerUri();
        if (issuer != null && !issuer.isBlank()) {
            return issuer.trim();
        }
        return "registration:" + userRequest.getClientRegistration().getRegistrationId();
    }

    private String requiredAttribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue.trim();
        }
        throw authenticationFailure("invalid_oauth_identity", "Required OAuth attribute is missing: " + name);
    }

    private OAuth2AuthenticationException authenticationFailure(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), description);
    }

    protected OAuth2User getDelegateUser(OAuth2UserRequest userRequest) {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        return delegate.loadUser(userRequest);
    }
}
