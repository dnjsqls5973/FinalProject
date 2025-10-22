package Wonbin.FinalProject.ai.quest.dto;

import Wonbin.FinalProject.ai.quest.domain.Quest;
import Wonbin.FinalProject.ai.quest.domain.QuestCategory;
import Wonbin.FinalProject.ai.quest.domain.QuestStatus;
import Wonbin.FinalProject.ai.quest.domain.UserQuest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class QuestResponse {
    
    private Long questId;
    private String title;
    private String description;
    private QuestCategory category;
    private LocalDate questDate;
    private String youtubeUrl;
    
    public static QuestResponse from(Quest quest, UserQuest userQuest) {
        return QuestResponse.builder()
                .questId(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .category(quest.getCategory())
                .questDate(quest.getQuestDate())
                .youtubeUrl(quest.getYoutubeUrl())
                .build();
    }
}
