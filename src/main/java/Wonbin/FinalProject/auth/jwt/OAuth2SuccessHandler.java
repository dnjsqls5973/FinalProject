package Wonbin.FinalProject.auth.jwt;

import Wonbin.FinalProject.auth.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final Environment environment;

//    // application.yml에서 설정 가능
//    @Value("${app.frontend.url:http://localhost:8081}")
//    private String frontendUrl;

    @Value("${app.auth.cookie-enabled:true}")
    private boolean cookieEnabled;

    @Value("${app.auth.redirect-with-token:true}")
    private boolean redirectWithToken;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");

        // 사용자 이름 추출 (선택사항)
        String name = (String) oAuth2User.getAttributes().get("name");

        log.info("OAuth2 인증 성공! 사용자: {} ({})", email, name);

        // 토큰 생성
        String accessToken = jwtProvider.createAccessToken(email);
        String refreshToken = jwtProvider.createRefreshToken(email);

        log.info("JWT 토큰 생성 완료 - Access: {}..., Refresh: {}...",
                accessToken.substring(0, Math.min(accessToken.length(), 20)),
                refreshToken.substring(0, Math.min(refreshToken.length(), 20)));

        // Refresh Token DB 저장
        refreshTokenService.save(email, refreshToken);

        // User-Agent 체크하여 모바일 웹 감지
        String userAgent = request.getHeader("User-Agent");
        boolean isMobile = detectMobileWeb(userAgent);

        // 쿠키 설정 (옵션)
        if (cookieEnabled) {
            addTokenCookies(response, accessToken, refreshToken, isMobile);
        }

        // 리다이렉트 URL 결정
        String redirectUrl = buildRedirectUrl(accessToken, refreshToken, email);

        log.info("리다이렉트 URL: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private String buildRedirectUrl(String accessToken, String refreshToken, String email) {
        String baseUrl = getFrontendUrl();

        // URL 파라미터로 토큰 전달 (모바일 웹 친화적)
        if (redirectWithToken) {
            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            urlBuilder.append("/auth/callback");
            urlBuilder.append("?token=").append(URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
            urlBuilder.append("&refreshToken=").append(URLEncoder.encode(refreshToken, StandardCharsets.UTF_8));
            urlBuilder.append("&email=").append(URLEncoder.encode(email, StandardCharsets.UTF_8));

            return urlBuilder.toString();
        } else {
            // 쿠키만 사용하는 경우
            return baseUrl + "/dashboard";
        }
    }

    private String getFrontendUrl() {
        // 환경별 프론트엔드 URL 결정
        String[] profiles = environment.getActiveProfiles();

        if (profiles.length == 0 || "local".equals(profiles[0]) || "dev".equals(profiles[0])) {
            return "http://localhost:8081";  // 개발 환경
        } else if ("staging".equals(profiles[0])) {
            return "https://staging.your-domain.com";  // 스테이징
        } else {
            return "https://your-domain.com";  // 프로덕션
        }
    }

    private void addTokenCookies(HttpServletResponse response, String accessToken,
                                 String refreshToken, boolean isMobile) {
        // Access Token 쿠키
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(false);  // 개발용 (프로덕션에서는 true 권장)
        accessCookie.setSecure(isSecureEnvironment());  // HTTPS 환경에서만 true
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 60);  // 1시간

        // 모바일 웹에서는 SameSite 설정 주의
        if (!isMobile) {
            accessCookie.setAttribute("SameSite", "Lax");
        }

        // Refresh Token 쿠키
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);  // Refresh Token은 항상 HttpOnly
        refreshCookie.setSecure(isSecureEnvironment());
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);  // 7일

        if (!isMobile) {
            refreshCookie.setAttribute("SameSite", "Lax");
        }

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        log.info("쿠키 설정 완료: accessToken (HttpOnly: false), refreshToken (HttpOnly: true)");
    }

    private boolean detectMobileWeb(String userAgent) {
        if (userAgent == null) return false;

        // 모바일 브라우저 감지
        String ua = userAgent.toLowerCase();
        return ua.contains("mobile") ||
                ua.contains("android") ||
                ua.contains("iphone") ||
                ua.contains("ipad") ||
                ua.contains("windows phone");
    }

    private boolean isSecureEnvironment() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) return false;

        String profile = profiles[0];
        return "prod".equals(profile) || "production".equals(profile) || "staging".equals(profile);
    }

//    // 테스트용 성공 페이지 (백엔드 단독 테스트 시 사용)
//    public void sendTestSuccessPage(HttpServletResponse response, String email) throws IOException {
//        response.setContentType("text/html;charset=UTF-8");
//        response.getWriter().write("""
//            <!DOCTYPE html>
//            <html>
//            <head>
//                <meta charset="UTF-8">
//                <meta name="viewport" content="width=device-width, initial-scale=1.0">
//                <title>로그인 성공</title>
//                <style>
//                    body {
//                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
//                        display: flex;
//                        justify-content: center;
//                        align-items: center;
//                        min-height: 100vh;
//                        margin: 0;
//                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
//                    }
//                    .container {
//                        background: white;
//                        padding: 2rem;
//                        border-radius: 10px;
//                        box-shadow: 0 10px 25px rgba(0,0,0,0.1);
//                        text-align: center;
//                        max-width: 90%;
//                        width: 400px;
//                    }
//                    h1 { color: #333; margin-bottom: 0.5rem; }
//                    p { color: #666; margin: 1rem 0; }
//                    .token-info {
//                        background: #f5f5f5;
//                        padding: 1rem;
//                        border-radius: 5px;
//                        margin: 1rem 0;
//                        word-break: break-all;
//                        font-family: monospace;
//                        font-size: 0.9rem;
//                    }
//                    button {
//                        background: #667eea;
//                        color: white;
//                        border: none;
//                        padding: 0.75rem 1.5rem;
//                        border-radius: 5px;
//                        font-size: 1rem;
//                        cursor: pointer;
//                        transition: background 0.3s;
//                    }
//                    button:hover { background: #5a67d8; }
//                </style>
//            </head>
//            <body>
//                <div class="container">
//                    <h1>🎉 로그인 성공!</h1>
//                    <p>환영합니다, <strong>%s</strong>님</p>
//                    <div class="token-info">
//                        <strong>토큰이 쿠키에 저장되었습니다</strong><br>
//                        개발자 도구 → Application → Cookies에서 확인 가능
//                    </div>
//                    <button onclick="checkAuth()">인증 상태 확인</button>
//                    <button onclick="location.href='/'">홈으로</button>
//                </div>
//                <script>
//                    function checkAuth() {
//                        fetch('/api/auth/check', {
//                            credentials: 'include'
//                        })
//                        .then(res => res.json())
//                        .then(data => alert('인증 상태: ' + JSON.stringify(data, null, 2)))
//                        .catch(err => alert('오류: ' + err));
//                    }
//                </script>
//            </body>
//            </html>
//            """.formatted(email));
//    }
}