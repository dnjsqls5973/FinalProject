package Wonbin.FinalProject.config;

import Wonbin.FinalProject.auth.jwt.JwtAuthenticationFilter;
import Wonbin.FinalProject.auth.jwt.JwtProvider;
import Wonbin.FinalProject.auth.jwt.OAuth2SuccessHandler;
import Wonbin.FinalProject.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 인스턴스 주입
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtProvider jwtProvider;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")) // ✅ H2 콘솔 CSRF 예외
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())) // ✅ iframe 허용 (H2 콘솔 사용 위해)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/h2-console/**").permitAll() // ✅ H2 콘솔 경로 허용
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler) // ✅ OAuth2 로그인 성공 시 JWT 리턴 처리
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class); // 🔐 필터 등록;

        return http.build();
    }
}