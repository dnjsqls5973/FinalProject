package Wonbin.FinalProject.ai.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistoryResponse {
    
    private Long diaryId;
    private List<ChatMessageResponse> messages;
}
