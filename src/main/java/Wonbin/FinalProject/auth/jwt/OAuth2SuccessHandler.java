package Wonbin.FinalProject.auth.jwt;

import Wonbin.FinalProject.auth.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;  // ✅ 이것 추가!
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");

        log.info("OAuth2 인증 성공! 사용자: {}", email);

        String accessToken = jwtProvider.createAccessToken(email);
        String refreshToken = jwtProvider.createRefreshToken(email);

        log.info("JWT 토큰 생성 완료 - Access: {}..., Refresh: {}...",
                accessToken.substring(0, 20), refreshToken.substring(0, 20));

        refreshTokenService.save(email, refreshToken);

        // ✅ 쿠키에 토큰 저장
        addTokenCookies(response, accessToken, refreshToken);

        // ✅ 백엔드 테스트용으로 변경
        String redirectUrl = "http://localhost:8080/api/auth/success"; // 또는 /api/auth/success

        // ✅ 프론트엔드로 리다이렉트
        //String redirectUrl = "http://localhost:3000/dashboard";

        log.info("리다이렉트 URL: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // Access Token 쿠키
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(false); // 운영단계시 true로 변경 필요
        accessCookie.setSecure(false); // 개발환경용 (HTTPS가 아니므로)
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 60); // 1시간

        // Refresh Token 쿠키
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(false); // 운영단계시 true로 변경 필요
        refreshCookie.setSecure(false); // 개발환경용
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7일

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        log.info("쿠키 설정 완료: accessToken, refreshToken");
    }
}