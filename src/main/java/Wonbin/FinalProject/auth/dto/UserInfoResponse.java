package Wonbin.FinalProject.auth.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserInfoResponse {
    private String email;
    private String name;
    private String roll;

    public UserInfoResponse(String email, String name, String roll) {
        this.email = email;
        this.name = name;
        this.roll = roll;
    }
}