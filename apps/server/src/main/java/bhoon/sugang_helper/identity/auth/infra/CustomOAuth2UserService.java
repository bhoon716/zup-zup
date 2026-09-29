package bhoon.sugang_helper.identity.auth.infra;

import bhoon.sugang_helper.identity.auth.contract.OAuth2AuthenticatedUser;
import bhoon.sugang_helper.identity.user.contract.AuthenticatedUser;
import bhoon.sugang_helper.identity.user.contract.OAuthAccountProvisioner;
import bhoon.sugang_helper.identity.user.contract.OAuthAccountProvisioningException;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuthAccountProvisioner accountProvisioner;

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
        AuthenticatedUser user;
        try {
            user = accountProvisioner.findOrCreate(issuer, subject, email, name);
        } catch (OAuthAccountProvisioningException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(exception.errorCode()), exception.getMessage());
        }
        log.info("Social login load complete (OAuth2): userId={}", user.id());

        return new OAuth2AuthenticatedUser(
                user.id(), user.email(), user.roleKey(), attributes, userNameAttributeName);
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
