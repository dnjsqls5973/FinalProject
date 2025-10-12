package Wonbin.FinalProject.ai.quest.repository;

import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.ai.quest.domain.Quest;
import Wonbin.FinalProject.ai.quest.domain.UserQuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {
    
    // 특정 사용자의 특정 퀘스트 조회
    Optional<UserQuest> findByUserAndQuest(User user, Quest quest);
}
