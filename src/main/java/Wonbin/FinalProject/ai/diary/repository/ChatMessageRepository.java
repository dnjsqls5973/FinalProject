package Wonbin.FinalProject.ai.diary.repository;

import Wonbin.FinalProject.ai.diary.domain.ChatMessage;
import Wonbin.FinalProject.ai.diary.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * 특정 일기의 모든 대화 메시지 조회 (시간순)
     */
    List<ChatMessage> findByDiaryOrderByCreatedAtAsc(Diary diary);
    
    /**
     * 특정 일기의 대화 메시지 삭제
     */
    void deleteByDiary(Diary diary);
}
