package bhoon.sugang_helper.identity.auth.config;

import bhoon.sugang_helper.identity.auth.client.CustomOAuth2UserService;
import bhoon.sugang_helper.identity.auth.handler.OAuth2FailureHandler;
import bhoon.sugang_helper.identity.auth.handler.OAuth2SuccessHandler;
import bhoon.sugang_helper.identity.auth.session.SessionAuthenticationFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String COURSE_SEARCH_PATH = "/api/v1/courses/search";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String GOOGLE_REGISTRATION_ID = "google";
    private static final String GOOGLE_PROMPT_PARAMETER = "prompt";
    private static final String GOOGLE_ACCOUNT_SELECTION_PROMPT = "select_account";

    private static final String[] PERMIT_ALL_ENDPOINTS = new String[]{
            "/api/health",
            "/health",
            "/error",
            "/favicon.ico",
            "/oauth2/**"
    };

    private static final String[] INTERNAL_MANAGEMENT_ENDPOINTS = new String[]{
            "/actuator/health",
            "/actuator/health/readiness",
            "/actuator/prometheus"
    };

    private static final String[] DEVELOPER_TOOL_ENDPOINTS = new String[]{
            "/h2-console/**"
    };

    private static final List<String> CORS_ALLOWED_METHODS = List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    private static final List<String> CORS_ALLOWED_HEADERS = List.of("Content-Type", "X-CSRF-TOKEN");

    private static final String[] PERMIT_GET_ENDPOINTS = new String[]{
            "/api/auth/csrf",
            "/api/v1/dashboard",
            "/api/v1/dashboard/landing-stats",
            "/api/v1/courses/**",
            "/api/v1/announcements/**",
            "/api/v1/schedules/**"
    };

    private static final String[] PERMIT_POST_ENDPOINTS = new String[]{
            "/api/auth/logout",
            COURSE_SEARCH_PATH
    };
    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final AccessDeniedHandler customAccessDeniedHandler;
    private final AuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${app.cors.require-https:false}")
    private boolean requireHttpsOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository securityContextRepository,
                                          CsrfTokenRepository csrfTokenRepository,
                                          ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
                        .ignoringRequestMatchers(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, COURSE_SEARCH_PATH)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .securityContext(context -> context.securityContextRepository(securityContextRepository))

                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PERMIT_ALL_ENDPOINTS).permitAll();
                    auth.requestMatchers(INTERNAL_MANAGEMENT_ENDPOINTS).permitAll();
                    auth.requestMatchers(DEVELOPER_TOOL_ENDPOINTS).hasRole(ADMIN_ROLE);
                    auth.requestMatchers(HttpMethod.GET, PERMIT_GET_ENDPOINTS).permitAll();
                    auth.requestMatchers(HttpMethod.POST, PERMIT_POST_ENDPOINTS).permitAll();
                    auth.requestMatchers("/api/v1/admin/**").hasRole(ADMIN_ROLE);
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(customAuthenticationEntryPoint))
                .headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin))
                .addFilterBefore(sessionAuthenticationFilter, CsrfFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(
                                authorizationRequestResolver(clientRegistrationRepository)))
                        .securityContextRepository(securityContextRepository)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler));

        return http.build();
    }

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        resolver.setAuthorizationRequestCustomizer(builder -> builder.attributes(attributes -> {
            if (GOOGLE_REGISTRATION_ID.equals(attributes.get(OAuth2ParameterNames.REGISTRATION_ID))) {
                builder.additionalParameters(parameters -> parameters.put(
                        GOOGLE_PROMPT_PARAMETER, GOOGLE_ACCOUNT_SELECTION_PROMPT));
            }
        }));
        return resolver;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = createCorsConfiguration(allowedOrigins, requireHttpsOrigins);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    static CorsConfiguration createCorsConfiguration(String[] configuredOrigins, boolean requireHttps) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(validateAllowedOrigins(configuredOrigins, requireHttps));
        configuration.setAllowedMethods(CORS_ALLOWED_METHODS);
        configuration.setAllowedHeaders(CORS_ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.validateAllowCredentials();
        return configuration;
    }

    private static List<String> validateAllowedOrigins(String[] configuredOrigins, boolean requireHttps) {
        if (configuredOrigins == null || configuredOrigins.length == 0) {
            throw new IllegalArgumentException("CORS 허용 출처를 하나 이상 지정해야 합니다.");
        }

        return Arrays.stream(configuredOrigins)
                .map(origin -> validateAllowedOrigin(origin, requireHttps))
                .distinct()
                .toList();
    }

    private static String validateAllowedOrigin(String configuredOrigin, boolean requireHttps) {
        if (!StringUtils.hasText(configuredOrigin)) {
            throw new IllegalArgumentException("CORS 허용 출처에는 공백만 입력할 수 없습니다.");
        }

        String origin = configuredOrigin.trim();
        if (origin.contains("*")) {
            throw new IllegalArgumentException("CORS 허용 출처에 와일드카드를 사용할 수 없습니다.");
        }

        URI uri = parseOrigin(origin);
        if (uri.getHost() == null || hasOriginPathOrExtraComponents(uri)) {
            throw new IllegalArgumentException("CORS 허용 출처에는 스킴, 호스트, 선택적 포트만 지정할 수 있습니다.");
        }
        if (!isHttpOrigin(uri)) {
            throw new IllegalArgumentException("CORS 허용 출처는 HTTP 또는 HTTPS를 사용해야 합니다.");
        }
        if (requireHttps && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("운영 환경의 CORS 허용 출처는 HTTPS를 사용해야 합니다.");
        }

        return origin;
    }

    private static URI parseOrigin(String origin) {
        try {
            return new URI(origin);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("CORS 허용 출처는 유효한 URI여야 합니다.", exception);
        }
    }

    private static boolean hasOriginPathOrExtraComponents(URI uri) {
        return !uri.isAbsolute()
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || uri.getUserInfo() != null;
    }

    private static boolean isHttpOrigin(URI uri) {
        return "http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme());
    }
}
