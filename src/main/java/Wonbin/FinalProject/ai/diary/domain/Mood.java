package Wonbin.FinalProject.ai.diary.domain;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Mood {
    VERY_HAPPY("very_happy"),
    HAPPY("happy"),
    NEUTRAL("neutral"),
    SAD("sad"),
    VERY_SAD("very_sad");

    private final String key;


    Mood(String key) {
        this.key = key;
    }

    /**
     * 프론트엔드에서 보낸 key 값으로 Mood 찾기
     */
    public static Mood fromKey(String key) {
        return Arrays.stream(values())
                .filter(m -> m.key.equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid mood: " + key));
    }
}
