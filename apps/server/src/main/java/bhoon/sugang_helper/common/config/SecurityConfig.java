package bhoon.sugang_helper.common.config;

import bhoon.sugang_helper.common.security.exception.CustomAccessDeniedHandler;
import bhoon.sugang_helper.common.security.exception.CustomAuthenticationEntryPoint;
import bhoon.sugang_helper.common.security.jwt.JwtAuthenticationFilter;
import bhoon.sugang_helper.common.security.oauth.CustomOAuth2UserService;
import bhoon.sugang_helper.common.security.oauth.OAuth2FailureHandler;
import bhoon.sugang_helper.common.security.oauth.OAuth2SuccessHandler;
import java.util.Arrays;
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

        private static final String[] PERMIT_ALL_ENDPOINTS = new String[] {
                        "/api/health",
                        "/health",
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/h2-console/**",
                        "/error",
                        "/favicon.ico",
                        "/oauth2/**"
        };

        private static final String[] PERMIT_GET_ENDPOINTS = new String[] {
                        "/api/v1/courses/**"
        };

        private static final String[] PERMIT_POST_ENDPOINTS = new String[] {
                        "/api/auth/refresh",
                        "/api/auth/logout"
        };

        @Value("${app.cors.allowed-origins}")
        private String[] allowedOrigins;

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final CustomAccessDeniedHandler customAccessDeniedHandler;
        private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;
        private final OAuth2FailureHandler oAuth2FailureHandler;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(PERMIT_ALL_ENDPOINTS).permitAll()
                                                .requestMatchers(HttpMethod.GET, PERMIT_GET_ENDPOINTS).permitAll()
                                                .requestMatchers(HttpMethod.POST, PERMIT_POST_ENDPOINTS).permitAll()
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                                .anyRequest().authenticated())
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

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
