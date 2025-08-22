package Wonbin.FinalProject.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserInfoResponse {
    private String email;
    private String name;

    public UserInfoResponse(String email, String name) {
        this.email = email;
        this.name = name;
    }
}