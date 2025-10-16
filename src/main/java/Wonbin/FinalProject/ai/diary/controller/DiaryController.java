package Wonbin.FinalProject.ai.diary.controller;

import Wonbin.FinalProject.ai.diary.dto.ChatResponse;
import Wonbin.FinalProject.ai.diary.dto.DiaryRequest;
import Wonbin.FinalProject.ai.diary.dto.DiaryResponse;
import Wonbin.FinalProject.ai.diary.service.DiaryChatService;
import Wonbin.FinalProject.ai.diary.service.DiaryService;
import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
@Slf4j
public class DiaryController {

    private final DiaryService diaryService;
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
     * 일기 저장 후 AI 대화 시작
     * POST /api/diary/save-and-chat
     */
    @PostMapping("/save-and-chat")
    public ResponseEntity<ChatResponse> saveDiaryAndStartChat(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody DiaryRequest request
    ) {
        log.info("[save-and-chat] email={}", email);
        
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = getUserIdFromEmail(email);

        DiaryResponse savedDiary = diaryService.saveDiary(userId, request);
        ChatResponse chatResponse = diaryChatService.startDiaryChat(userId, savedDiary.getDiaryDate());
        
        log.info("일기 저장 및 AI 대화 시작 완료 - userId: {}", userId);
        return ResponseEntity.ok(chatResponse);
    }

    /**
     * 일기 작성/수정
     * POST /api/diary
     */
    @PostMapping
    public ResponseEntity<DiaryResponse> saveDiary(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody DiaryRequest request
    ) {
        Long userId = getUserIdFromEmail(email);
        DiaryResponse response = diaryService.saveDiary(userId, request);

        log.info("일기 저장 완료 - userId: {}", userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 날짜의 일기 조회
     * GET /api/diary?date=2025-01-15
     */
    @GetMapping
    public ResponseEntity<DiaryResponse> getDiary(
            @AuthenticationPrincipal String email,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserIdFromEmail(email);
        DiaryResponse response = diaryService.getDiary(userId, date);

        return ResponseEntity.ok(response);
    }

    /**
     * 모든 일기 조회 (최신순)
     * GET /api/diary/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<DiaryResponse>> getAllDiaries(
            @AuthenticationPrincipal String email
    ) {
        Long userId = getUserIdFromEmail(email);
        List<DiaryResponse> response = diaryService.getAllDiaries(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 기간의 일기 조회
     * GET /api/diary/range?startDate=2025-01-01&endDate=2025-01-31
     */
    @GetMapping("/range")
    public ResponseEntity<List<DiaryResponse>> getDiariesByDateRange(
            @AuthenticationPrincipal String email,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = getUserIdFromEmail(email);
        List<DiaryResponse> response = diaryService.getDiariesByDateRange(userId, startDate, endDate);

        return ResponseEntity.ok(response);
    }

    /**
     * 일기 삭제
     * DELETE /api/diary/{diaryId}
     */
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @AuthenticationPrincipal String email,
            @PathVariable Long diaryId
    ) {
        Long userId = getUserIdFromEmail(email);
        diaryService.deleteDiary(userId, diaryId);

        log.info("일기 삭제 완료 - userId: {}, diaryId: {}", userId, diaryId);
        return ResponseEntity.noContent().build();
    }
}
