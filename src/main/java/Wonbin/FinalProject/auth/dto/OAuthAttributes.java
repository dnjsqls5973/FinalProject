package Wonbin.FinalProject.auth.dto;

import lombok.Getter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private String providerId;
    private String email;
    private String name;

    public static OAuthAttributes of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> ofGoogle(attributes);
            case "kakao" -> ofKakao(attributes);
            case "naver" -> ofNaver(attributes);
            default -> throw new OAuth2AuthenticationException("Unknown provider: " + registrationId);
        };
    }

    private static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
        return new OAuthAttributes(
                (String) attributes.get("sub"),
                (String) attributes.get("email"),
                (String) attributes.get("name")
        );
    }

    private static OAuthAttributes ofKakao(Map<String, Object> attributes) {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        return new OAuthAttributes(
                String.valueOf(attributes.get("id")),
                (String) account.get("email"),
                (String) profile.get("nickname")
        );
    }

    private static OAuthAttributes ofNaver(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return new OAuthAttributes(
                (String) response.get("id"),
                (String) response.get("email"),
                (String) response.get("name")
        );
    }

    public OAuthAttributes(String providerId, String email, String name) {
        this.providerId = providerId;
        this.email = email;
        this.name = name;
    }
}