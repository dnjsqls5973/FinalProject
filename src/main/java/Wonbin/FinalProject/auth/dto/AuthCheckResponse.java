package Wonbin.FinalProject.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// DTO 클래스들 (별도 파일로 분리 권장)
public class AuthCheckResponse {
    private boolean authenticated;
    private String email;
    private String reason;

    public AuthCheckResponse(boolean authenticated, String email, String reason) {
        this.authenticated = authenticated;
        this.email = email;
        this.reason = reason;
    }
}
