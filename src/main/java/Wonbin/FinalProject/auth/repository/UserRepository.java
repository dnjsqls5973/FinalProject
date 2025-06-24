package Wonbin.FinalProject.auth.repository;

import Wonbin.FinalProject.auth.common.AuthProvider;
import Wonbin.FinalProject.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(AuthProvider authProvider, String providerId);
}
