package bhoon.sugang_helper.common.config;

import bhoon.sugang_helper.common.security.exception.CustomAccessDeniedHandler;
import bhoon.sugang_helper.common.security.exception.CustomAuthenticationEntryPoint;
import bhoon.sugang_helper.common.security.jwt.JwtAuthenticationFilter;
import bhoon.sugang_helper.common.security.oauth.CustomOAuth2UserService;
import bhoon.sugang_helper.common.security.oauth.OAuth2FailureHandler;
import bhoon.sugang_helper.common.security.oauth.OAuth2SuccessHandler;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PERMIT_ALL_ENDPOINTS = new String[]{
            "/api/health",
            "/health",
            "/error",
            "/favicon.ico",
            "/oauth2/**"
    };

    private static final String[] INTERNAL_MANAGEMENT_ENDPOINTS = new String[]{
            "/actuator/health",
            "/actuator/prometheus"
    };

    private static final String[] DEVELOPER_TOOL_ENDPOINTS = new String[]{
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/h2-console/**"
    };

    private static final List<String> CORS_ALLOWED_METHODS = List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    private static final List<String> CORS_ALLOWED_HEADERS = List.of("Content-Type");

    private static final String[] PERMIT_GET_ENDPOINTS = new String[]{
            "/api/v1/dashboard",
            "/api/v1/ddays/**",
            "/api/v1/courses/**",
            "/api/v1/announcements/**",
            "/api/v1/schedules/**"
    };

    private static final String[] PERMIT_POST_ENDPOINTS = new String[]{
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/v1/courses/search"
    };
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;
    @Value("${app.cors.require-https:false}")
    private boolean requireHttpsOrigins;

    /**
     * 비밀번호 암호화를 위한 Encoder 빈을 등록합니다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * HTTP 보안 필터 체인을 구성합니다.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PERMIT_ALL_ENDPOINTS).permitAll();
                    auth.requestMatchers(INTERNAL_MANAGEMENT_ENDPOINTS).permitAll();
                    auth.requestMatchers(DEVELOPER_TOOL_ENDPOINTS).hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.GET, PERMIT_GET_ENDPOINTS).permitAll();
                    auth.requestMatchers(HttpMethod.POST, PERMIT_POST_ENDPOINTS).permitAll();
                    auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(customAuthenticationEntryPoint))
                .headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler));

        return http.build();
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 설정을 구성합니다.
     */
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
            throw new IllegalArgumentException("CORS allowed origins must not be empty");
        }

        return Arrays.stream(configuredOrigins)
                .map(origin -> validateAllowedOrigin(origin, requireHttps))
                .distinct()
                .toList();
    }

    private static String validateAllowedOrigin(String configuredOrigin, boolean requireHttps) {
        if (configuredOrigin == null || configuredOrigin.isBlank()) {
            throw new IllegalArgumentException("CORS allowed origin must not be blank");
        }

        String origin = configuredOrigin.trim();
        if (origin.contains("*")) {
            throw new IllegalArgumentException("CORS allowed origin must not contain a wildcard");
        }

        URI uri = parseOrigin(origin);
        if (uri.getHost() == null || hasOriginPathOrExtraComponents(uri)) {
            throw new IllegalArgumentException("CORS allowed origin must contain only scheme, host, and optional port");
        }
        if (!isHttpOrigin(uri)) {
            throw new IllegalArgumentException("CORS allowed origin must use HTTP or HTTPS");
        }
        if (requireHttps && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Production CORS allowed origins must use HTTPS");
        }

        return origin;
    }

    private static URI parseOrigin(String origin) {
        try {
            return new URI(origin);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("CORS allowed origin must be a valid URI", exception);
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
