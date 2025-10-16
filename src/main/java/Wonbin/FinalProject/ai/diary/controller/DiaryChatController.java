package Wonbin.FinalProject.ai.diary.controller;

import Wonbin.FinalProject.ai.diary.dto.ChatRequest;
import Wonbin.FinalProject.ai.diary.dto.ChatResponse;
import Wonbin.FinalProject.ai.diary.service.DiaryChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/diary/chat")
@RequiredArgsConstructor
@Slf4j
public class DiaryChatController {

    private final DiaryChatService diaryChatService;

    /**
     * 일기 기반 대화 시작
     * POST /api/diary/chat/start?date=2025-01-15
     * 
     * 특정 날짜의 일기를 기반으로 AI 상담사와의 대화를 시작합니다.
     */
    @PostMapping("/start")
    public ResponseEntity<ChatResponse> startChat(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam LocalDate date
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        ChatResponse response = diaryChatService.startDiaryChat(userId, date);
        
        log.info("일기 기반 채팅 시작 - userId: {}, date: {}", userId, date);
        return ResponseEntity.ok(response);
    }

    /**
     * 대화 계속하기
     * POST /api/diary/chat
     * 
     * 사용자의 메시지를 받아 AI 상담사의 응답을 반환합니다.
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChatRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        ChatResponse response = diaryChatService.continueChat(userId, request);
        
        log.info("채팅 메시지 전송 - userId: {}", userId);
        return ResponseEntity.ok(response);
    }
}
