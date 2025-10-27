package Wonbin.FinalProject.ai.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    private Long diaryId; // 일기 ID 추가!
    private String message; // AI 상담사의 응답
    private String mood; // 현재 대화 중인 일기의 감정
}
