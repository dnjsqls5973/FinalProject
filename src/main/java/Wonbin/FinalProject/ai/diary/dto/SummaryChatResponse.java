package Wonbin.FinalProject.ai.diary.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryChatResponse {
    private String summary;  // 요약 텍스트
}