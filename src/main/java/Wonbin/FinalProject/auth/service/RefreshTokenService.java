package Wonbin.FinalProject.auth.service;

import Wonbin.FinalProject.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Refresh Token 저장
     */
    public void save(String email, String refreshToken) {
        try {
            refreshTokenRepository.save(email, refreshToken);
            log.info("Refresh Token 저장 완료: {}", email);
        } catch (Exception e) {
            log.error("Refresh Token 저장 실패: {}, 오류: {}", email, e.getMessage());
            throw new RuntimeException("Refresh Token 저장에 실패했습니다.", e);
        }
    }

    /**
     * 이메일로 Refresh Token 조회
     */
    public Optional<String> findByEmail(String email) {
        return refreshTokenRepository.findByEmail(email);
    }

    /**
     * ✅ Refresh Token 유효성 검증 (중요!)
     * DB에 저장된 토큰과 일치하는지 확인
     */
    public boolean validateToken(String email, String refreshToken) {
        Optional<String> storedToken = findByEmail(email);

        if (storedToken.isEmpty()) {
            log.warn("저장된 Refresh Token이 없습니다: {}", email);
            return false;
        }

        boolean isValid = storedToken.get().equals(refreshToken);
        if (!isValid) {
            log.warn("Refresh Token이 일치하지 않습니다: {}", email);
        }

        return isValid;
    }

    /**
     * ✅ 메서드명 통일 (delete)
     */
    public void delete(String email) {
        deleteByEmail(email);
    }

    /**
     * Refresh Token 삭제
     */
    public void deleteByEmail(String email) {
        try {
            refreshTokenRepository.deleteByEmail(email);
            log.info("Refresh Token 삭제 완료: {}", email);
        } catch (Exception e) {
            log.error("Refresh Token 삭제 실패: {}, 오류: {}", email, e.getMessage());
        }
    }

    /**
     * ✅ 토큰 존재 여부 확인
     */
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    /**
     * ✅ 토큰 교체 (기존 토큰 삭제 후 새 토큰 저장)
     */
    public void replace(String email, String oldRefreshToken, String newRefreshToken) {
        if (!validateToken(email, oldRefreshToken)) {
            throw new IllegalArgumentException("기존 Refresh Token이 유효하지 않습니다.");
        }

        deleteByEmail(email);
        save(email, newRefreshToken);
        log.info("Refresh Token 교체 완료: {}", email);
    }

    /**
     * ✅ 만료된 토큰 정리 (스케줄러에서 사용)
     */
    public void cleanupExpiredTokens() {
        // 구현은 Repository의 저장 방식에 따라 달라짐
        log.info("만료된 Refresh Token 정리 작업 실행");
    }
}