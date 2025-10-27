package Wonbin.FinalProject.ai.diary.dto;

import lombok.*;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryChatRequest {
    private List<Message> messages;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}