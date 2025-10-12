package Wonbin.FinalProject.ai.quest.domain;

import Wonbin.FinalProject.auth.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quests",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quest_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Setter
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestCategory category;

    @Column(nullable = false)
    private LocalDate questDate;

    @Column(name = "title_embedding", columnDefinition = "TEXT")
    private String titleEmbedding;  // 🔥 Embedding 저장 (JSON 형식)

    @Column(length = 500)
    private String youtubeUrl;  // 유튜브 링크

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Embedding을 float 배열로 변환
     */
    public float[] getTitleEmbeddingArray() {
        if (titleEmbedding == null) return null;
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(titleEmbedding, float[].class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * float 배열을 JSON 문자열로 저장
     */
    public void setTitleEmbeddingArray(float[] embedding) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.titleEmbedding = mapper.writeValueAsString(embedding);
        } catch (Exception e) {
            this.titleEmbedding = null;
        }
    }

}
