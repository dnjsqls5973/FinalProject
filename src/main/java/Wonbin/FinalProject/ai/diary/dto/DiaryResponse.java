package Wonbin.FinalProject.ai.diary.dto;

import Wonbin.FinalProject.ai.diary.domain.Diary;
import Wonbin.FinalProject.ai.diary.domain.Mood;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryResponse {

    private Long id;
    private LocalDate diaryDate;
    private String mood; // Enum key 값
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> Response DTO 변환
     */
    public static DiaryResponse from(Diary diary) {
        Mood mood = diary.getMood();
        
        return DiaryResponse.builder()
                .id(diary.getId())
                .diaryDate(diary.getDiaryDate())
                .mood(mood.getKey())
                .content(diary.getContent())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
