package Wonbin.FinalProject.ai.diary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryRequest {
    
    @NotBlank(message = "요약 텍스트는 필수입니다.")
    private String summaryText;
}
