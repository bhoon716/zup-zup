package bhoon.sugang_helper.identity.auth.client;

import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import bhoon.sugang_helper.identity.auth.service.OAuth2AuthenticatedUser;
import bhoon.sugang_helper.identity.user.dto.result.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.exception.OAuthAccountProvisioningException;
import bhoon.sugang_helper.identity.user.service.UserOAuthAccountService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String EVENT_KEY = "event";

    private final UserOAuthAccountService accountProvisioner;

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

        logOauthLoginRequested(registrationId, SensitiveDataRedactor.fingerprint(email));
        AuthenticatedUser user;
        try {
            user = accountProvisioner.findOrCreate(issuer, subject, email, name);
        } catch (OAuthAccountProvisioningException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(exception.errorCode()), exception.getMessage());
        }
        logOauthLoginUserFound(user.id());

        return new OAuth2AuthenticatedUser(
                user.id(), user.email(), user.roleKey(), attributes, userNameAttributeName);
    }

    private String providerIssuer(OAuth2UserRequest userRequest) {
        String issuer = userRequest.getClientRegistration().getProviderDetails().getIssuerUri();
        if (StringUtils.hasText(issuer)) {
            return issuer.trim();
        }
        return "registration:" + userRequest.getClientRegistration().getRegistrationId();
    }

    private String requiredAttribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue.trim();
        }
        throw authenticationFailure("invalid_oauth_identity", "필수 OAuth 속성이 누락됐습니다: " + name);
    }

    private OAuth2AuthenticationException authenticationFailure(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), description);
    }

    protected OAuth2User getDelegateUser(OAuth2UserRequest userRequest) {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        return delegate.loadUser(userRequest);
    }

    private void logOauthLoginRequested(String provider, String emailFingerprint) {
        log.atInfo().addKeyValue(EVENT_KEY, "oauth_login_requested")
                .addKeyValue("provider", provider)
                .addKeyValue("emailFingerprint", emailFingerprint)
                .log("[OAuth2] 외부 계정 로그인 요청.");
    }

    private void logOauthLoginUserFound(Long userId) {
        log.atInfo().addKeyValue(EVENT_KEY, "oauth_login_user_found")
                .addKeyValue("userId", userId)
                .log("[OAuth2] 로그인 사용자 조회 완료.");
    }

}
