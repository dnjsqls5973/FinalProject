package Wonbin.FinalProject.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenEntity {

    @Id
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ 편의 생성자 (이메일, 토큰만으로 생성)
    public RefreshTokenEntity(String email, String token) {
        this.email = email;
        this.token = token;
        this.expiresAt = LocalDateTime.now().plusDays(7); // 7일 후 만료
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ JPA 생명주기 콜백
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // 만료시간이 설정되지 않았다면 7일 후로 설정
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusDays(7);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ 토큰 만료 여부 확인
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    // ✅ 토큰 업데이트
    public void updateToken(String newToken) {
        this.token = newToken;
        this.expiresAt = LocalDateTime.now().plusDays(7);
        this.updatedAt = LocalDateTime.now();
    }
}