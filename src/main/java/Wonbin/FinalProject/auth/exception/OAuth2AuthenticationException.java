package Wonbin.FinalProject.auth.exception;

/**
 * OAuth2 인증 실패 시 발생하는 예외
 */
public class OAuth2AuthenticationException extends RuntimeException {
    
    public OAuth2AuthenticationException(String message) {
        super(message);
    }
    
    public OAuth2AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
