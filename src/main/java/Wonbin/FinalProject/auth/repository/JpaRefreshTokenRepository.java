package Wonbin.FinalProject.auth.repository;

import Wonbin.FinalProject.auth.domain.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {

    // ✅ 기본 메서드들 (JpaRepository에서 자동 제공)
    // - save(RefreshTokenEntity entity)
    // - findById(String email)
    // - deleteById(String email)

    // ✅ 이메일로 토큰 조회 (만료되지 않은 것만)
    @Query("SELECT r FROM RefreshTokenEntity r WHERE r.email = :email AND r.expiresAt > :now")
    Optional<RefreshTokenEntity> findValidTokenByEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    // ✅ 만료된 토큰들 삭제 (정리 작업용)
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    // ✅ 특정 사용자의 만료된 토큰 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.email = :email AND r.expiresAt < :now")
    int deleteExpiredTokensByEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    // ✅ 토큰 존재 여부 확인
    boolean existsByEmail(String email);

    // ✅ 만료시간으로 토큰 조회
    @Query("SELECT r FROM RefreshTokenEntity r WHERE r.expiresAt > :now")
    Optional<RefreshTokenEntity> findAllValidTokens(@Param("now") LocalDateTime now);
}