package Wonbin.FinalProject.ai.quest.controller;

import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.ai.quest.dto.QuestResponse;
import Wonbin.FinalProject.ai.quest.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    /**
     * 오늘의 퀘스트 조회
     */
    @GetMapping("/today")
    public ResponseEntity<QuestResponse> getTodayQuest(
            @AuthenticationPrincipal String email) {  // User -> String으로 변경

        // null 체크 추가
        if (email == null) {
            return ResponseEntity.status(401)
                    .body(null);
        }

        QuestResponse quest = questService.getTodayQuestByEmail(email);  // 이메일로 조회
        return ResponseEntity.ok(quest);
    }

    /**
     * 퀘스트 완료 처리
     */
    @PostMapping("/{questId}/complete")
    public ResponseEntity<String> completeQuest(
            @AuthenticationPrincipal String email,  // User -> String으로 변경
            @PathVariable Long questId) {
        questService.completeQuestByEmail(email, questId);  // 이메일로 처리
        return ResponseEntity.ok("Quest completed successfully!");
    }
}
