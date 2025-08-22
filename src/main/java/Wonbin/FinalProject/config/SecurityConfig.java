package Wonbin.FinalProject.config;

import Wonbin.FinalProject.auth.jwt.JwtAuthenticationFilter;
import Wonbin.FinalProject.auth.jwt.JwtProvider;
import Wonbin.FinalProject.auth.jwt.OAuth2SuccessHandler;
import Wonbin.FinalProject.auth.service.CustomOAuth2UserService;
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

    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1) REST API + JWT 이면 보통 전역 CSRF OFF
                .csrf(csrf -> csrf.disable())

                // 2) 브라우저 호출할 경우 CORS 필요 (CorsConfigurationSource 빈도 함께 등록 권장)
                .cors(Customizer.withDefaults())

                // 3) 세션 미사용 (Stateless)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4) H2 콘솔(개발용)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                // 5) URL 인가 정책
                .authorizeHttpRequests(auth -> auth
                        // OAuth2 진입/콜백, 토큰 리프레시 등 공개
                        .requestMatchers("/", "/login", "/oauth2/**", "/api/token/refresh").permitAll()
                        // H2 콘솔(개발)
                        .requestMatchers("/h2-console/**").permitAll()

                        // 👉 공개 API가 있으면 여기에 추가 (예: /chat, /summarize)
                        .requestMatchers("/chat", "/summarize").permitAll()

                        // Token Test
                        .requestMatchers("/test.html", "/api/test/public", "/api/test/auth-status","api/auth/**").permitAll()

                        // 나머지는 JWT 필요
                        .anyRequest().authenticated()
                )

                // 6) JWT 필터 등록 (UsernamePasswordAuthenticationFilter 앞)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 7) OAuth2 로그인: 유저정보 조회 + 성공 시 JWT 발급/리다이렉트
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(ui -> ui.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )

                // 8) 예외 처리
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            String uri = req.getRequestURI();
                            if (uri.startsWith("/oauth2/") || uri.startsWith("/login")) {
                                res.sendRedirect("/oauth2/authorization/google");
                                return;
                            }
                            res.setContentType("application/json");
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + e.getMessage() + "\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setContentType("application/json");
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"" + e.getMessage() + "\"}");
                        })
                );

        return http.build();
    }

    // 프론트 엔드 연결 시 필요한 CORS
//    @Bean
//    CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowCredentials(true);
//        config.setAllowedOrigins(List.of("http://localhost:3000", "https://your-domain.com"));
//        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
//        config.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With"));
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }
}
