package Wonbin.FinalProject.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String response;

    public TokenResponse(String accessToken, String refreshToken, String response) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.response = response;
    }
}