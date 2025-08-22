package Wonbin.FinalProject.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;

    // ✅ 적절한 만료시간 설정
    private final long accessTokenExpireMs = 1000L * 60 * 60;      // 1시간
    private final long refreshTokenExpireMs = 1000L * 60 * 60 * 24 * 7; // 7일

    // ✅ 생성자에서 SecretKey 초기화 (최신 JJWT 방식)
    public JwtProvider(@Value("${jwt.secret}") String secret) {
        // 최소 32바이트(256비트) 필요
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ✅ Access Token 생성
    public String createAccessToken(String userEmail) {
        String token = createToken(userEmail, accessTokenExpireMs, "ACCESS");
        log.info("[AccessToken 발급] {}", token.substring(0, 15));
        return token;
    }

    // ✅ Refresh Token 생성
    public String createRefreshToken(String userEmail) {
        String token = createToken(userEmail, refreshTokenExpireMs, "REFRESH");
        log.info("[RefreshToken 발급] {}", token.substring(0, 15));
        return token;
    }

    // ✅ 개선된 토큰 생성 로직
    // ✅ 개선된 토큰 생성 로직
    private String createToken(String userEmail, long expireTime, String tokenType) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireTime);

        // ✅ 고유성을 위한 추가 정보
        String jti = UUID.randomUUID().toString(); // JWT ID
        long nanoTime = System.nanoTime(); // 나노초 시간

        log.info("토큰 생성 시작 - Type: {}, Email: {}, Time: {}", tokenType, userEmail, now.getTime());

        String token = Jwts.builder()
                .setSubject(userEmail)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .claim("type", tokenType)  // 토큰 타입 구분
                .claim("jti", jti)         // ✅ 고유 ID 추가
                .claim("nano", nanoTime)   // ✅ 나노초 시간 추가 (고유성 보장)
                .signWith(secretKey)       // 최신 API 사용
                .compact();

        log.info("토큰 생성 완료 - Type: {}, JTI: {}, Token: {}", tokenType, jti, token.substring(0, 50) + "...");
        return token;
    }

    // ✅ 토큰에서 이메일 추출
    public String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    // ✅ 토큰 타입 확인
    public String getTokenType(String token) {
        return getClaims(token).get("type", String.class);
    }

    // ✅ 토큰 만료시간 확인
    public Date getExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    // ✅ 토큰 유효성 검증 (상세한 예외 처리)
    public boolean validate(String token) {
        try {
            getClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            System.err.println("잘못된 JWT 서명입니다: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("만료된 JWT 토큰입니다: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("지원되지 않는 JWT 토큰입니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT 토큰이 잘못되었습니다: " + e.getMessage());
        }
        return false;
    }

    // ✅ Access Token 전용 검증
    public boolean validateAccessToken(String token) {
        if (!validate(token)) return false;

        String tokenType = getTokenType(token);
        return "ACCESS".equals(tokenType);
    }

    // ✅ Refresh Token 전용 검증
    public boolean validateRefreshToken(String token) {
        if (!validate(token)) return false;

        String tokenType = getTokenType(token);
        return "REFRESH".equals(tokenType);
    }

    // ✅ Claims 추출 (공통 로직)
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ✅ 토큰 만료까지 남은 시간 (밀리초)
    public long getTimeUntilExpiration(String token) {
        Date expiration = getExpiration(token);
        return expiration.getTime() - System.currentTimeMillis();
    }
}