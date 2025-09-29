package Wonbin.FinalProject.auth.repository;

import Wonbin.FinalProject.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * OAuth 제공자와 제공자 ID로 사용자 조회
     */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    /**
     * 제공자별 사용자 조회
     */
    Optional<User> findByProvider(String provider);
}