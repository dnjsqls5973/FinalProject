package Wonbin.FinalProject.config;

import Wonbin.FinalProject.auth.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1) REST API + JWT 이면 보통 전역 CSRF OFF
                .csrf(csrf -> csrf.disable())

                // 2) 브라우저 호출할 경우 CORS 필요
                .cors(Customizer.withDefaults())

                // 3) 세션 미사용 (Stateless)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4) H2 콘솔(개발용)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                // 5) URL 인가 정책
                .authorizeHttpRequests(auth -> auth
                        // 🔥 인증 관련 API 모두 허용
                        .requestMatchers("/api/auth/**").permitAll()

                        // OAuth2 진입점들 (혹시 Spring OAuth2도 함께 사용할 경우)
                        .requestMatchers("/", "/login", "/oauth2/**").permitAll()

                        // H2 콘솔(개발)
                        .requestMatchers("/h2-console/**").permitAll()

                        // 공개 API
                        .requestMatchers("/chat", "/summarize").permitAll()

                        // 테스트 API
                        .requestMatchers("/test.html", "/api/test/public", "/api/test/auth-status").permitAll()

                        // 나머지는 JWT 필요
                        .anyRequest().authenticated()
                )

                // 6) JWT 필터 등록 (UsernamePasswordAuthenticationFilter 앞)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 7) 예외 처리 - 인증 관련 API는 리다이렉트 하지 않음
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            String uri = req.getRequestURI();

                            // 🔥 API 요청인 경우 JSON 응답
                            if (uri.startsWith("/api/")) {
                                res.setContentType("application/json; charset=UTF-8");
                                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다\"}");
                                return;
                            }

                            // OAuth2 관련 요청인 경우에만 Google 로그인으로 리다이렉트
                            if (uri.startsWith("/oauth2/") || uri.equals("/login")) {
                                res.sendRedirect("/api/auth/google/url");
                                return;
                            }

                            // 일반 페이지 요청인 경우
                            res.setContentType("application/json; charset=UTF-8");
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setContentType("application/json; charset=UTF-8");
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"접근 권한이 없습니다\"}");
                        })
                );

        return http.build();
    }

    // 프론트엔드 연결 시 필요한 CORS 설정
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8081"
                // "https://your-domain.com" // 프로덕션 도메인 추가
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}