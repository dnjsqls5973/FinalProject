package Wonbin.FinalProject.ai.diary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    
    @NotBlank(message = "역할은 필수입니다.")
    @Pattern(regexp = "^(user|assistant)$", message = "역할은 user 또는 assistant만 가능합니다.")
    private String role;
    
    @NotBlank(message = "메시지 내용은 필수입니다.")
    private String content;
}
