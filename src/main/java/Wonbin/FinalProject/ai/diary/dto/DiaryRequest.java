package Wonbin.FinalProject.ai.diary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryRequest {

    @NotNull(message = "날짜는 필수입니다.")
    private String date; // ISO 8601 형식 (yyyy-MM-dd)

    @NotBlank(message = "감정은 필수입니다.")
    private String mood; // very_happy, happy, neutral, sad, very_sad

    @NotBlank(message = "일기 내용은 필수입니다.")
    @Size(min = 1, max = 5000, message = "일기 내용은 1자 이상 5000자 이하여야 합니다.")
    private String content;
}
