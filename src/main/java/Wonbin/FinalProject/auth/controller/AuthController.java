package Wonbin.FinalProject.auth.controller;

import Wonbin.FinalProject.auth.dto.TokenResponse;
import Wonbin.FinalProject.auth.dto.UserInfoResponse;
import Wonbin.FinalProject.auth.dto.AuthCheckResponse;
import Wonbin.FinalProject.auth.jwt.JwtProvider;
import Wonbin.FinalProject.auth.service.RefreshTokenService;
import Wonbin.FinalProject.auth.service.UserService;
import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.ai.quest.service.QuestService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final QuestService questService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    // ✅ Google OAuth 로그인 URL 제공
    @GetMapping("/oauth2/authorization/google")
    public ResponseEntity<Map<String, String>> getGoogleLoginUrl() {
        String googleAuthUrl = "https://accounts.google.com/o/oauth2/auth?" +
                "client_id=" + googleClientId +
                "&redirect_uri=https://unstraddled-frenzily-jerome.ngrok-free.dev/api/auth/google/callback" +
                "&scope=openid+profile+email" +
                "&response_type=code" +
                "&access_type=offline" +
                "&prompt=consent";

        Map<String, String> response = new HashMap<>();
        response.put("url", googleAuthUrl);

        log.info("Google OAuth URL 생성: {}", googleAuthUrl);
        return ResponseEntity.ok(response);
    }

    // ✅ 헬스 체크 (프론트엔드 연결 확인용)
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Backend server is running");
        response.put("timestamp", System.currentTimeMillis());
        response.put("frontendUrl", frontendUrl);
        return ResponseEntity.ok(response);
    }

    // ✅ Google OAuth 콜백 처리
    @GetMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestParam String code, HttpServletResponse response) {
        try {
            log.info("Google OAuth 콜백 받음 - code: {}", code.substring(0, Math.min(code.length(), 20)) + "...");

            // Google에서 사용자 정보 가져와서 처리
            User user = userService.processGoogleLogin(code);

            // JWT 토큰 생성
            String accessToken = jwtProvider.createAccessToken(user.getEmail());
            String refreshToken = jwtProvider.createRefreshToken(user.getEmail());

            // Refresh Token 저장
            refreshTokenService.save(user.getEmail(), refreshToken);

            // 🔥 비동기로 퀘스트 미리 생성 (백그라운드에서 실행)
            questService.prepareQuestAsync(user);
            log.info("🎯 Quest generation started in background for user: {}", user.getEmail());

            // 쿠키에 토큰 저장
            addTokenCookies(response, accessToken, refreshToken);

            log.info("Google 로그인 성공 - 사용자: {}", user.getEmail());


//            // 프론트엔드로 리다이렉트
//            return ResponseEntity.status(302)
//                    .location(URI.create(frontendUrl + "/auth/socialCallback"/*"/pages/Home"*/))
//                    .build();
            // 쿠키가 아닌 헤더로 토큰 전달
            return ResponseEntity.status(302)
                    .location(URI.create(frontendUrl + "/auth/socialCallback?accessToken=" + accessToken + "&refreshToken=" + refreshToken))
                    .build();

        } catch (Exception e) {
            log.error("Google OAuth 콜백 처리 중 오류 발생", e);
//            return ResponseEntity.status(302)
//                    .location(URI.create(frontendUrl + "/auth/error?message=" + e.getMessage()))
//                    .build();
            // 쿠키가 아닌 헤더로 토큰 전달
            return ResponseEntity.status(302)
                    .location(URI.create(frontendUrl + "/auth/error?message=" +
                            URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)))
                    .build();
        }
    }

    // ✅ 인증 상태 확인 API (페이지 새로고침 시 사용)
    @GetMapping("/check")
    public ResponseEntity<AuthCheckResponse> checkAuth(HttpServletRequest request) {
        String token = resolveAccessToken(request);

        if (!StringUtils.hasText(token)) {
            log.debug("토큰이 없음");
            return ResponseEntity.ok(new AuthCheckResponse(false, null, null));
        }

        if (!jwtProvider.validateAccessToken(token)) {
            log.debug("유효하지 않은 토큰");
            return ResponseEntity.ok(new AuthCheckResponse(false, null, "EXPIRED"));
        }

        String email = jwtProvider.getEmail(token);
        log.info("인증 상태 확인 - 사용자: {}", email);

        return ResponseEntity.ok(new AuthCheckResponse(true, email, null));
    }

    // ✅ 토큰 갱신 API
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Refresh Token 추출 (Body > Header > Cookie 순서)
        String refreshToken = null;

        // 1. Request Body에서 확인 (모바일 웹 친화적)
        if (body != null && body.containsKey("refreshToken")) {
            refreshToken = body.get("refreshToken");
        }

        // 2. 헤더나 쿠키에서 확인
        if (!StringUtils.hasText(refreshToken)) {
            refreshToken = resolveRefreshToken(request);
        }

        if (!StringUtils.hasText(refreshToken)) {
            log.warn("Refresh Token이 없음");
            return ResponseEntity.badRequest().body(new TokenResponse(null, null, "REFRESH_TOKEN_MISSING"));
        }

        // Refresh Token 검증
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            log.warn("유효하지 않은 Refresh Token");
            return ResponseEntity.status(401).body(new TokenResponse(null, null, "INVALID_REFRESH_TOKEN"));
        }

        String email = jwtProvider.getEmail(refreshToken);

        // DB에 저장된 Refresh Token과 일치하는지 확인
        if (!refreshTokenService.validateToken(email, refreshToken)) {
            log.warn("DB에 저장된 토큰과 불일치 - 사용자: {}", email);
            return ResponseEntity.status(401).body(new TokenResponse(null, null, "TOKEN_MISMATCH"));
        }

        // 새로운 토큰 발급
        String newAccessToken = jwtProvider.createAccessToken(email);
        String newRefreshToken = jwtProvider.createRefreshToken(email);

        // 새 Refresh Token 저장
        refreshTokenService.save(email, newRefreshToken);
        log.info("토큰 갱신 성공 - 사용자: {}", email);

        // 쿠키로도 토큰 전달
        addTokenCookies(response, newAccessToken, newRefreshToken);

        return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken, "SUCCESS"));
    }

    // ✅ 로그아웃 API
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = null;

        // Request Body에서 먼저 확인
        if (body != null && body.containsKey("refreshToken")) {
            refreshToken = body.get("refreshToken");
        }

        // 헤더나 쿠키에서 확인
        if (!StringUtils.hasText(refreshToken)) {
            refreshToken = resolveRefreshToken(request);
        }

        if (StringUtils.hasText(refreshToken) && jwtProvider.validateRefreshToken(refreshToken)) {
            String email = jwtProvider.getEmail(refreshToken);
            refreshTokenService.delete(email); // DB에서 Refresh Token 삭제
            log.info("로그아웃 성공 - 사용자: {}", email);
        }

        // 쿠키 삭제
        clearTokenCookies(response);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "로그아웃되었습니다");

        return ResponseEntity.ok(result);
    }

    // ✅ 현재 사용자 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser(HttpServletRequest request) {
        String token = resolveAccessToken(request);

        if (!StringUtils.hasText(token) || !jwtProvider.validateAccessToken(token)) {
            return ResponseEntity.status(401).build();
        }

        String email = jwtProvider.getEmail(token);

        try {
            User user = userService.findByEmail(email);
            log.info("사용자 정보 조회 - {}", email);

            return ResponseEntity.ok(new UserInfoResponse(
                    user.getEmail(),
                    user.getName(),
                    user.getRole().toString()
            ));
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패 - {}", email, e);
            return ResponseEntity.status(404).build();
        }
    }

    // ===== Helper Methods =====

    private String resolveRefreshToken(HttpServletRequest request) {
        // 헤더에서 먼저 확인
        String headerToken = request.getHeader("Refresh-Token");
        if (StringUtils.hasText(headerToken)) {
            return headerToken;
        }

        // 쿠키에서 확인
        return getTokenFromCookie(request, "refreshToken");
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

    private void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // ✅ Access Token 쿠키 (JavaScript에서 접근 가능)
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(false);  // JavaScript 접근 가능
        accessCookie.setSecure(true);    // 개발환경: false, 프로덕션: true
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 60);  // 1시간

        // ✅ Refresh Token 쿠키 (보안 강화)
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);   // JavaScript 접근 불가 (보안)
        refreshCookie.setSecure(true);    // 개발환경: false, 프로덕션: true
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7일

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        // ⬅️ 변경: SameSite=None 추가 (크로스 도메인 허용)
        response.setHeader("Set-Cookie",
                String.format("accessToken=%s; Path=/; Max-Age=%d; Secure; SameSite=None",
                        accessToken, 60 * 60));
        response.addHeader("Set-Cookie",
                String.format("refreshToken=%s; Path=/; Max-Age=%d; HttpOnly; Secure; SameSite=None",
                        refreshToken, 7 * 24 * 60 * 60));

//        // ✅ SameSite 속성 추가 (CSRF 방어)
//        response.setHeader("Set-Cookie",
//            String.format("accessToken=%s; Path=/; Max-Age=%d; SameSite=Lax",
//                accessToken, 60 * 60));
//        response.addHeader("Set-Cookie",
//            String.format("refreshToken=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Strict",
//                refreshToken, 7 * 24 * 60 * 60));
    }

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