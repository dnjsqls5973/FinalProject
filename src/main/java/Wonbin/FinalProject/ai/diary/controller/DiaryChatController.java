package Wonbin.FinalProject.ai.diary.controller;

import Wonbin.FinalProject.ai.diary.dto.ChatRequest;
import Wonbin.FinalProject.ai.diary.dto.ChatResponse;
import Wonbin.FinalProject.ai.diary.dto.ChatHistoryResponse;
import Wonbin.FinalProject.ai.diary.dto.SummaryChatResponse;
import Wonbin.FinalProject.ai.diary.service.DiaryChatService;
import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/diary/chat")
@RequiredArgsConstructor
@Slf4j
public class DiaryChatController {

    private final DiaryChatService diaryChatService;
    private final UserRepository userRepository;

    /**
     * Helper: email로 userId 조회
     */
    private Long getUserIdFromEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return user.getId();
    }

    /**
     * 일기 기반 대화 시작 (자동 저장)
     * POST /api/diary/chat/start?date=2025-01-15
     * 
     * 특정 날짜의 일기를 기반으로 AI 상담사와의 대화를 시작하고 DB에 저장합니다.
     */
    @PostMapping("/start")
    public ResponseEntity<ChatResponse> startChat(
            @AuthenticationPrincipal String email,
            @RequestParam LocalDate date
    ) {
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = getUserIdFromEmail(email);
        ChatResponse response = diaryChatService.startDiaryChat(userId, date);
        
        log.info("일기 기반 채팅 시작 - userId: {}, date: {}", userId, date);
        return ResponseEntity.ok(response);
    }

    /**
     * 대화 계속하기 (자동 저장)
     * POST /api/diary/chat/{diaryId}
     * 
     * 사용자의 메시지를 받아 AI 상담사의 응답을 반환하고 DB에 저장합니다.
     */
    @PostMapping("/{diaryId}")
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal String email,
            @PathVariable Long diaryId,
            @Valid @RequestBody ChatRequest request
    ) {
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = getUserIdFromEmail(email);
        ChatResponse response = diaryChatService.continueChat(userId, diaryId, request);
        
        log.info("채팅 메시지 전송 및 저장 - userId: {}, diaryId: {}", userId, diaryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 대화 요약 생성
     * POST /api/diary/chat/{diaryId}/summary
     */
    @PostMapping("/{diaryId}/summary")
    public ResponseEntity<SummaryChatResponse> summarizeChat(
            @AuthenticationPrincipal String email,
            @PathVariable Long diaryId
    ) {
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = getUserIdFromEmail(email);
        SummaryChatResponse response = diaryChatService.summarizeChat(userId, diaryId);

        log.info("대화 요약 생성 - userId: {}, diaryId: {}", userId, diaryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 대화 내역 조회
     * GET /api/diary/chat/{diaryId}
     */
    @GetMapping("/{diaryId}")
    public ResponseEntity<ChatHistoryResponse> getChatHistory(
            @AuthenticationPrincipal String email,
            @PathVariable Long diaryId
    ) {
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = getUserIdFromEmail(email);
        ChatHistoryResponse response = diaryChatService.getChatHistory(userId, diaryId);

        log.info("대화 내역 조회 - userId: {}, diaryId: {}", userId, diaryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 대화 내역 삭제
     * DELETE /api/diary/chat/{diaryId}
     */
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteChatHistory(
            @AuthenticationPrincipal String email,
            @PathVariable Long diaryId
    ) {
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = getUserIdFromEmail(email);
        diaryChatService.deleteChatHistory(userId, diaryId);

        log.info("대화 내역 삭제 - userId: {}, diaryId: {}", userId, diaryId);
        return ResponseEntity.noContent().build();
    }
}
