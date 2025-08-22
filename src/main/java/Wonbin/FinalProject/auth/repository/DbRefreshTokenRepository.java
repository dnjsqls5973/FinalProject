package Wonbin.FinalProject.auth.repository;

import Wonbin.FinalProject.auth.domain.RefreshTokenEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DbRefreshTokenRepository implements RefreshTokenRepository {

    private final JpaRefreshTokenRepository jpaRepository;

    @Override
    public void save(String email, String refreshToken) {
        try {
            // 기존 토큰이 있으면 업데이트, 없으면 새로 생성
            Optional<RefreshTokenEntity> existingToken = jpaRepository.findById(email);

            if (existingToken.isPresent()) {
                // 기존 토큰 업데이트
                RefreshTokenEntity entity = existingToken.get();
                entity.updateToken(refreshToken);
                jpaRepository.save(entity);
                log.info("Refresh Token 업데이트됨: {}", email);
            } else {
                // 새 토큰 생성
                RefreshTokenEntity newEntity = new RefreshTokenEntity(email, refreshToken);
                jpaRepository.save(newEntity);
                log.info("새 Refresh Token 저장됨: {}", email);
            }
        } catch (Exception e) {
            log.error("Refresh Token 저장 실패: {}, 오류: {}", email, e.getMessage());
            throw new RuntimeException("Refresh Token 저장에 실패했습니다.", e);
        }
    }

    @Override
    public Optional<String> findByEmail(String email) {
        try {
            // 만료되지 않은 유효한 토큰만 조회
            Optional<RefreshTokenEntity> entity = jpaRepository.findValidTokenByEmail(email, LocalDateTime.now());

            if (entity.isPresent()) {
                log.info("유효한 Refresh Token 조회됨: {}", email);
                return Optional.of(entity.get().getToken());
            } else {
                log.info("유효한 Refresh Token이 없음: {}", email);
                // 만료된 토큰이 있다면 삭제
                cleanupExpiredTokenForUser(email);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Refresh Token 조회 실패: {}, 오류: {}", email, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void deleteByEmail(String email) {
        try {
            if (jpaRepository.existsByEmail(email)) {
                jpaRepository.deleteById(email);
                log.info("Refresh Token 삭제됨: {}", email);
            } else {
                log.info("삭제할 Refresh Token이 없음: {}", email);
            }
        } catch (Exception e) {
            log.error("Refresh Token 삭제 실패: {}, 오류: {}", email, e.getMessage());
        }
    }

    // ✅ 특정 사용자의 만료된 토큰 정리
    private void cleanupExpiredTokenForUser(String email) {
        try {
            int deletedCount = jpaRepository.deleteExpiredTokensByEmail(email, LocalDateTime.now());
            if (deletedCount > 0) {
                log.info("만료된 토큰 정리됨: {} ({}개)", email, deletedCount);
            }
        } catch (Exception e) {
            log.warn("만료된 토큰 정리 실패: {}, 오류: {}", email, e.getMessage());
        }
    }

    // ✅ 전체 만료된 토큰 정리 (스케줄러용)
    public int cleanupAllExpiredTokens() {
        try {
            int deletedCount = jpaRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("전체 만료된 토큰 정리 완료: {}개", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("만료된 토큰 정리 실패: {}", e.getMessage());
            return 0;
        }
    }
}