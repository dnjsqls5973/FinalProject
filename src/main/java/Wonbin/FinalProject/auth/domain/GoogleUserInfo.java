package Wonbin.FinalProject.auth.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * Google 사용자 정보를 담는 내부 클래스
 */
@Builder
@Getter
public class GoogleUserInfo {
    private String email;
    private String name;
    private String picture;
    private String googleId;
}
