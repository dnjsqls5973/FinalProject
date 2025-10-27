package Wonbin.FinalProject.ai.diary.controller;

import Wonbin.FinalProject.ai.diary.dto.ChatHistoryResponse;
import Wonbin.FinalProject.ai.diary.dto.ChatMessageRequest;
import Wonbin.FinalProject.ai.diary.dto.ChatMessageResponse;
import Wonbin.FinalProject.ai.diary.service.ChatMessageService;
import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diary/{diaryId}/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

    private final UserRepository userRepository;
    private final ChatMessageService chatMessageService;

    /**
     * Helper: email로 userId 조회
     */
    private Long getUserIdFromEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return user.getId();
    }

    /**
     * 챗봇 메시지 저장
     * POST /api/diary/{diaryId}/chat
     */
    @PostMapping
    public ResponseEntity<ChatMessageResponse> saveMessage(
            @AuthenticationPrincipal String email,
            @PathVariable Long diaryId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = getUserIdFromEmail(email);
        ChatMessageResponse response = chatMessageService.saveMessage(userId, diaryId, request);

        log.info("챗봇 메시지 저장 - userId: {}, diaryId: {}", userId, diaryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 대화 내역 조회
     * GET /api/diary/{diaryId}/chat
     */
    @GetMapping
    public ResponseEntity<ChatHistoryResponse> getChatHistory(
            @AuthenticationPrincipal String email,
            @PathVariable Long diaryId
    ) {
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = getUserIdFromEmail(email);
        ChatHistoryResponse response = chatMessageService.getChatHistory(userId, diaryId);

        return ResponseEntity.ok(response);
    }

    /**
     * 대화 내역 삭제
     * DELETE /api/diary/{diaryId}/chat
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteChatHistory(
            @AuthenticationPrincipal String email,
            @PathVariable Long diaryId
    ) {
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = getUserIdFromEmail(email);
        chatMessageService.deleteChatHistory(userId, diaryId);

        log.info("챗봇 대화 내역 삭제 - userId: {}, diaryId: {}", userId, diaryId);
        return ResponseEntity.noContent().build();
    }
}
