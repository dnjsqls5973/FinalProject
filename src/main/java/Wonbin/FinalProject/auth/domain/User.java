package Wonbin.FinalProject.auth.domain;

import Wonbin.FinalProject.auth.common.AuthProvider;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String name;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;    // GOOGLE, KAKAO, NAVER 등

    private String providerId;        // 각 플랫폼의 사용자 ID
}
