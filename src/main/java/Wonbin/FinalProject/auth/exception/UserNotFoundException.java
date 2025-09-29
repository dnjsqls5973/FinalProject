package Wonbin.FinalProject.auth.exception;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 */
public class UserNotFoundException extends RuntimeException {
    
    private String email;
    
    /**
     * 일반 메시지로 예외 생성
     */
    public UserNotFoundException(String message) {
        super(message);
    }
    
    /**
     * 이메일 정보를 포함한 예외 생성
     */
    public UserNotFoundException(String message, String email) {
        super(message);
        this.email = email;
    }
    
    /**
     * 이메일로 기본 메시지를 가진 예외 생성 (정적 팩토리 메서드)
     */
    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("사용자를 찾을 수 없습니다: " + email, email);
    }
    
    public String getEmail() {
        return email;
    }
}
