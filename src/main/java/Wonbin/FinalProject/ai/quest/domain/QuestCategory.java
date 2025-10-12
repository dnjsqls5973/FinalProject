package Wonbin.FinalProject.ai.quest.domain;

public enum QuestCategory {
    HEALTH("건강"),
    LEARNING("학습"),
    SOCIAL("사회성"),
    CREATIVE("창의성"),
    DAILY_LIFE("일상"),
    MINDFULNESS("마음챙김");

    private final String korean;

    QuestCategory(String korean) {
        this.korean = korean;
    }

    public String getKorean() {
        return korean;
    }
}
