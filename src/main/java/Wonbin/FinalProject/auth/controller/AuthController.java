package Wonbin.FinalProject.auth.controller;

import Wonbin.FinalProject.auth.dto.TokenResponse;
import Wonbin.FinalProject.auth.dto.UserInfoResponse;
import Wonbin.FinalProject.auth.jwt.JwtProvider;
import Wonbin.FinalProject.auth.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    // ✅ 토큰 갱신 API
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {

        // Refresh Token 추출 (쿠키 또는 헤더)
        String refreshToken = resolveRefreshToken(request);

        if (!StringUtils.hasText(refreshToken)) {
            return ResponseEntity.badRequest().build();
        }

        // Refresh Token 검증
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).build();
        }

        String email = jwtProvider.getEmail(refreshToken);

        // DB에 저장된 Refresh Token과 일치하는지 확인
        if (!refreshTokenService.validateToken(email, refreshToken)) {
            return ResponseEntity.status(401).build();
        }

        // 새로운 토큰 발급
        String newAccessToken = jwtProvider.createAccessToken(email);
        String newRefreshToken = jwtProvider.createRefreshToken(email);

        // 새 Refresh Token 저장
        refreshTokenService.save(email, newRefreshToken);

        // 쿠키로 토큰 전달하는 경우
        addTokenCookies(response, newAccessToken, newRefreshToken);

        return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken));
    }

    // ✅ 로그아웃 API
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = resolveRefreshToken(request);

        if (StringUtils.hasText(refreshToken)) {
            String email = jwtProvider.getEmail(refreshToken);
            refreshTokenService.delete(email); // DB에서 Refresh Token 삭제
        }

        // 쿠키 삭제
        clearTokenCookies(response);

        return ResponseEntity.ok().build();
    }

    // ✅ 현재 사용자 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser(HttpServletRequest request) {
        String token = resolveAccessToken(request);

        if (!StringUtils.hasText(token) || !jwtProvider.validateAccessToken(token)) {
            return ResponseEntity.status(401).build();
        }

        String email = jwtProvider.getEmail(token);
        // 여기서 UserService를 통해 사용자 정보 조회
        // User user = userService.findByEmail(email);

        return ResponseEntity.ok(new UserInfoResponse(email, "사용자 이름"));
    }

    // ✅ 토큰 추출 메서드들
    private String resolveRefreshToken(HttpServletRequest request) {
        // 헤더에서 먼저 확인
        String headerToken = request.getHeader("Refresh-Token");
        if (StringUtils.hasText(headerToken)) {
            return headerToken;
        }

        // 쿠키에서 확인
        return getTokenFromCookie(request, "refreshToken");
    }

    @GetMapping("/success")
    public ResponseEntity<String> authSuccess(HttpServletRequest request) {
        // 쿠키에서 토큰 확인
        String accessToken = getTokenFromCookie(request, "accessToken");
        String refreshToken = getTokenFromCookie(request, "refreshToken");

        String html = """
            <html>
            <head><title>OAuth2 인증 성공</title></head>
            <body>
                <h1>🎉 OAuth2 인증 성공!</h1>
                <p>Access Token: %s</p>
                <p>Refresh Token: %s</p>
                <a href="/api/auth/me">사용자 정보 확인</a>
            </body>
            </html>
            """.formatted(
                accessToken != null ? accessToken.substring(0, 20) + "..." : "없음",
                refreshToken != null ? refreshToken.substring(0, 20) + "..." : "없음"
        );

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    private String resolveAccessToken(HttpServletRequest request) {
        // Authorization 헤더에서 먼저 확인
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 쿠키에서 확인
        return getTokenFromCookie(request, "accessToken");
    }

    private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // ✅ 쿠키에 토큰 저장
    private void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(false);
        accessCookie.setSecure(false); // 개발환경용
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 60); // 1시간

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(false);
        refreshCookie.setSecure(false); // 개발환경용
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7일

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    // ✅ 쿠키 삭제
    private void clearTokenCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("accessToken", "");
        accessCookie.setMaxAge(0);
        accessCookie.setPath("/");

        Cookie refreshCookie = new Cookie("refreshToken", "");
        refreshCookie.setMaxAge(0);
        refreshCookie.setPath("/");

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }
}