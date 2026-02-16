package bhoon.sugang_helper.common.security.oauth;

import bhoon.sugang_helper.domain.user.entity.Role;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = getDelegateUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        log.info("소셜 로그인 사용자 정보 로드 요청(OAuth2): 제공자={}, 이메일={}", registrationId, email);
        User user = saveOrUpdate(email, name);
        log.info("소셜 로그인 사용자 정보 로드 완료(OAuth2): 사용자ID={}", user.getId());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey())),
                attributes,
                userNameAttributeName);
    }

    private User saveOrUpdate(String email, String name) {
        User user = userRepository.findByEmail(email)
                .orElse(User.builder()
                        .email(email)
                        .name(name)
                        .role(Role.USER)
                        .build());

        User savedUser = userRepository.save(user);
        log.info("사용자 저장/갱신 완료: 사용자ID={}, 이메일={}", savedUser.getId(), email);
        return savedUser;
    }

    protected OAuth2User getDelegateUser(OAuth2UserRequest userRequest) {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        return delegate.loadUser(userRequest);
    }
}
