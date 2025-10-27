package Wonbin.FinalProject.ai.diary.domain;

import Wonbin.FinalProject.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "diaries",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "diary_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate diaryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mood mood;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 챗봇 대화 요약
    @Column(columnDefinition = "TEXT")
    private String summaryText;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 일기 내용 및 감정 업데이트
     */
    public void updateDiary(Mood mood, String content) {
        this.mood = mood;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 요약 텍스트 업데이트
     */
    public void updateSummary(String summaryText) {
        this.summaryText = summaryText;
        this.updatedAt = LocalDateTime.now();
    }
}
