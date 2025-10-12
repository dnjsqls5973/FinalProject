package Wonbin.FinalProject.ai.quest.dto;

import Wonbin.FinalProject.ai.quest.domain.QuestCategory;
import lombok.Data;

@Data
public class QuestGenerationRequest {
    private String title;
    private String description;
    private QuestCategory category;
}
