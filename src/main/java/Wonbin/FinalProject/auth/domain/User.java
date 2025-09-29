package Wonbin.FinalProject.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private String picture;

    @Column(nullable = false)
    private String provider; // "GOOGLE", "KAKAO", "LOCAL" 등

    private String providerId; // OAuth 제공자의 사용자 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Google 로그인 시 사용자 정보 업데이트
     */
    public void updateGoogleInfo(String name, String picture) {
        this.name = name;
        this.picture = picture;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 사용자 이름 업데이트
     */
    public void updateName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 프로필 사진 업데이트
     */
    public void updatePicture(String picture) {
        this.picture = picture;
        this.updatedAt = LocalDateTime.now();
    }
}