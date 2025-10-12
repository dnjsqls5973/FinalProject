package Wonbin.FinalProject.ai.quest.repository;

import Wonbin.FinalProject.ai.quest.domain.Quest;
import Wonbin.FinalProject.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestRepository extends JpaRepository<Quest, Long> {
    
    // 🔥 특정 사용자의 특정 날짜 퀘스트 조회
    Optional<Quest> findByUserAndQuestDate(User user, LocalDate questDate);
    
    // 🔥 특정 사용자의 특정 날짜에 퀘스트가 있는지 확인
    boolean existsByUserAndQuestDate(User user, LocalDate questDate);
    
    // 🔥 특정 사용자의 날짜 범위 퀘스트 조회 (중복 체크용)
    List<Quest> findByUserAndQuestDateBetween(User user, LocalDate startDate, LocalDate endDate);
}
