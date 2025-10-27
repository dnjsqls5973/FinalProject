package Wonbin.FinalProject.ai.diary.service;

import Wonbin.FinalProject.ai.diary.domain.ChatMessage;
import Wonbin.FinalProject.ai.diary.domain.Diary;
import Wonbin.FinalProject.ai.diary.dto.ChatHistoryResponse;
import Wonbin.FinalProject.ai.diary.dto.ChatMessageRequest;
import Wonbin.FinalProject.ai.diary.dto.ChatMessageResponse;
import Wonbin.FinalProject.ai.diary.repository.ChatMessageRepository;
import Wonbin.FinalProject.ai.diary.repository.DiaryRepository;
import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    /**
     * 챗봇 메시지 저장
     */
    @Transactional
    public ChatMessageResponse saveMessage(Long userId, Long diaryId, ChatMessageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));

        // 본인의 일기인지 확인
        if (!diary.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 일기만 접근할 수 있습니다.");
        }

        // 메시지 저장
        ChatMessage message = ChatMessage.builder()
                .diary(diary)
                .role(request.getRole())
                .content(request.getContent())
                .build();

        message = chatMessageRepository.save(message);
        log.info("챗봇 메시지 저장 - diaryId: {}, role: {}", diaryId, request.getRole());

        return ChatMessageResponse.from(message);
    }

    /**
     * 특정 일기의 전체 대화 내역 조회
     */
    public ChatHistoryResponse getChatHistory(Long userId, Long diaryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));

        // 본인의 일기인지 확인
        if (!diary.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 일기만 접근할 수 있습니다.");
        }

        List<ChatMessage> messages = chatMessageRepository.findByDiaryOrderByCreatedAtAsc(diary);

        List<ChatMessageResponse> messageResponses = messages.stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());

        return ChatHistoryResponse.builder()
                .diaryId(diaryId)
                .messages(messageResponses)
                .build();
    }

    /**
     * 특정 일기의 대화 내역 삭제
     */
    @Transactional
    public void deleteChatHistory(Long userId, Long diaryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));

        // 본인의 일기인지 확인
        if (!diary.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 일기만 삭제할 수 있습니다.");
        }

        chatMessageRepository.deleteByDiary(diary);
        log.info("챗봇 대화 내역 삭제 - diaryId: {}", diaryId);
    }
}
