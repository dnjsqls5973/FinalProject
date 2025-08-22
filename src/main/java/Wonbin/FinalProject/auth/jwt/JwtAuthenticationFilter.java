package Wonbin.FinalProject.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            // ✅ Access Token인지 확인하고 유효성 검증
            if (StringUtils.hasText(token) && jwtProvider.validateAccessToken(token)) {
                String email = jwtProvider.getEmail(token);

                // ✅ 더 안전한 Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );

                // ✅ 추가 정보 설정 (선택사항)
                // authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // ✅ 더 상세한 로그 (개발환경에서만)
            logger.warn("JWT 인증 처리 중 오류 발생: " + e.getMessage());

            // 인증 실패시 SecurityContext 클리어
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    // ✅ 헤더와 쿠키 모두에서 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        // 1순위: Authorization 헤더에서 토큰 추출
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2순위: 쿠키에서 토큰 추출
        return getTokenFromCookie(request, "accessToken");
    }

    // ✅ 쿠키에서 토큰 추출
    private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // ✅ 특정 경로는 필터링 제외 (성능 향상)
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // OAuth2, 로그인, 정적 리소스는 JWT 필터링 제외
        return path.startsWith("/oauth2/") ||
                path.startsWith("/login") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/h2-console/") ||
                path.equals("/") ||
                path.startsWith("/api/auth/");
    }
}