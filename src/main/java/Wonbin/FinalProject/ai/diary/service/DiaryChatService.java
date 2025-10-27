package Wonbin.FinalProject.ai.diary.service;

import Wonbin.FinalProject.ai.common.service.OpenAIService;
import Wonbin.FinalProject.ai.diary.domain.Diary;
import Wonbin.FinalProject.ai.diary.domain.Mood;
import Wonbin.FinalProject.ai.diary.dto.*;
import Wonbin.FinalProject.ai.diary.repository.DiaryRepository;
import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryChatService {

    private final OpenAIService openAIService;
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final ChatMessageService chatMessageService; // ChatMessageService 통합!

    /**
     * 일기 기반 대화 시작 (DB 저장 포함)
     * 일기 내용과 감정을 기반으로 첫 AI 응답 생성 및 저장
     */
    @Transactional
    public ChatResponse startDiaryChat(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findByUserAndDiaryDate(user, date)
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 일기를 찾을 수 없습니다."));

        Mood mood = diary.getMood();
        String content = diary.getContent();

        log.info("일기 내용 확인 - mood: {}, content: {}", mood.getKey(), content); // 🔥 디버깅용

        // 시스템 프롬프트 생성 (상담사 느낌)
        String systemPrompt = buildCounselorPrompt(mood);

        // 🔥 일기 기반 첫 대화 시작 (프롬프트 개선)
        String initialPrompt = String.format(
                "%s\n\n" +
                        "사용자가 방금 다음과 같은 일기를 작성했습니다:\n" +
                        "===================\n" +
                        "감정: %s\n" +
                        "내용:\n%s\n" +
                        "===================\n\n" +
                        "위 일기의 구체적인 내용을 언급하며 공감하는 대화를 시작해주세요. " +
                        "일기에 나온 구체적인 상황이나 감정에 대해 자연스럽게 질문해주세요. " +
                        "절대 일반적인 질문을 하지 말고, 일기 내용과 직접 연관된 대화를 시작하세요.",
                systemPrompt,
                getMoodDisplayName(mood),  // "행복" 같은 한글 표현
                content
        );

        log.info("프롬프트 전송:\n{}", initialPrompt); // 🔥 프롬프트 확인

        // OpenAI API 호출
        String aiResponse = openAIService.chat(initialPrompt).block();

        log.info("AI 응답 받음: {}", aiResponse); // 🔥 응답 확인

        // AI 응답 DB에 저장
        ChatMessageRequest assistantMessage = new ChatMessageRequest("assistant", aiResponse);
        chatMessageService.saveMessage(userId, diary.getId(), assistantMessage);

        log.info("일기 기반 대화 시작 및 저장 - userId: {}, diaryId: {}, mood: {}",
                userId, diary.getId(), mood.getKey());

        return ChatResponse.builder()
                .diaryId(diary.getId())
                .message(aiResponse)
                .mood(mood.getKey())
                .build();
    }

    /**
     * Mood를 한글로 변환
     */
    private String getMoodDisplayName(Mood mood) {
        switch (mood) {
            case VERY_SAD: return "매우 슬픔";
            case SAD: return "슬픔";
            case NEUTRAL: return "보통";
            case HAPPY: return "행복";
            case VERY_HAPPY: return "매우 행복";
            default: return "알 수 없음";
        }
    }

    /**
     * 대화 계속하기 (DB 저장 포함)
     * 사용자 메시지와 AI 응답을 모두 DB에 자동 저장
     */
    @Transactional
    public ChatResponse continueChat(Long userId, Long diaryId, ChatRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));

        // 본인의 일기인지 확인
        if (!diary.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 일기만 접근할 수 있습니다.");
        }

        // 1. 사용자 메시지 저장
        ChatMessageRequest userMessage = new ChatMessageRequest("user", request.getMessage());
        chatMessageService.saveMessage(userId, diaryId, userMessage);

        // 2. DB에서 대화 내역 불러오기 (컨텍스트 구성)
        ChatHistoryResponse history = chatMessageService.getChatHistory(userId, diaryId);
        String context = buildContextFromHistory(history);

        Mood mood = diary.getMood();

        // 3. 상담사 프롬프트
        String systemPrompt = buildCounselorPrompt(mood);

        // 4. 대화 이어가기
        String fullPrompt = String.format(
            "%s\n\n이전 대화 맥락:\n%s\n\n사용자: %s\n\n자연스럽게 대화를 이어가며 공감하고, 필요하다면 적절한 질문을 던져주세요.",
            systemPrompt, context, request.getMessage()
        );

        // 5. OpenAI API 호출
        String aiResponse = openAIService.chat(fullPrompt).block();

        // 6. AI 응답 저장
        ChatMessageRequest assistantMessage = new ChatMessageRequest("assistant", aiResponse);
        chatMessageService.saveMessage(userId, diaryId, assistantMessage);

        log.info("대화 계속 및 저장 - userId: {}, diaryId: {}", userId, diaryId);

        return ChatResponse.builder()
                .diaryId(diaryId)
                .message(aiResponse)
                .mood(mood.getKey())
                .build();
    }

    /**
     * 대화 내역 요약 (GPT 사용) 및 DB 저장
     */
    @Transactional
    public SummaryChatResponse summarizeChat(Long userId, Long diaryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));

        // 본인의 일기인지 확인
        if (!diary.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 일기만 접근할 수 있습니다.");
        }

        // 대화 내역 조회
        ChatHistoryResponse history = chatMessageService.getChatHistory(userId, diaryId);

        if (history.getMessages().isEmpty()) {
            throw new IllegalArgumentException("요약할 대화 내역이 없습니다.");
        }

        // 대화 내역을 텍스트로 변환
        StringBuilder conversationText = new StringBuilder();
        for (ChatMessageResponse msg : history.getMessages()) {
            String speaker = msg.getRole().equals("user") ? "사용자" : "상담사";
            conversationText.append(speaker)
                    .append(": ")
                    .append(msg.getContent())
                    .append("\n\n");
        }

        // GPT에게 요약 요청
        String summaryPrompt = String.format(
                "다음은 사용자와 심리 상담사의 대화 내역입니다.\n\n" +
                        "%s\n" +
                        "위 대화를 3-5문장으로 요약해주세요. 다음 내용을 포함해야 합니다:\n" +
                        "1. 사용자가 나눈 주요 이야기\n" +
                        "2. 사용자의 감정 상태\n" +
                        "3. 대화를 통해 얻은 인사이트나 깨달음\n\n" +
                        "따뜻하고 공감하는 톤으로 작성하되, 객관적으로 요약해주세요.",
                conversationText.toString()
        );

        log.info("대화 요약 요청 - userId: {}, diaryId: {}", userId, diaryId);

        // OpenAI API 호출
        String summary = openAIService.chat(summaryPrompt).block();

        // DB에 저장
        diary.updateSummary(summary);
        diaryRepository.save(diary);
        
        log.info("대화 요약 저장 완료 - userId: {}, diaryId: {}", userId, diaryId);

        return SummaryChatResponse.builder()
                .summary(summary)
                .build();
    }

    /**
     * 대화 내역 조회 (ChatMessageService 위임)
     */
    public ChatHistoryResponse getChatHistory(Long userId, Long diaryId) {
        return chatMessageService.getChatHistory(userId, diaryId);
    }

    /**
     * 대화 내역 삭제 (ChatMessageService 위임)
     */
    @Transactional
    public void deleteChatHistory(Long userId, Long diaryId) {
        chatMessageService.deleteChatHistory(userId, diaryId);
    }

    /**
     * 대화 내역에서 컨텍스트 문자열 생성
     */
    private String buildContextFromHistory(ChatHistoryResponse history) {
        if (history.getMessages().isEmpty()) {
            return "대화 시작";
        }

        StringBuilder context = new StringBuilder();
        // 최근 10개 메시지만 사용 (너무 길면 토큰 초과)
        int startIndex = Math.max(0, history.getMessages().size() - 10);
        
        for (int i = startIndex; i < history.getMessages().size(); i++) {
            ChatMessageResponse msg = history.getMessages().get(i);
            String speaker = msg.getRole().equals("user") ? "사용자" : "AI";
            context.append(speaker).append(": ").append(msg.getContent()).append("\n");
        }

        return context.toString();
    }

    /**
     * 감정별 상담사 시스템 프롬프트
     */
    private String buildCounselorPrompt(Mood mood) {
        String basePrompt = "당신은 따뜻하고 공감 능력이 뛰어난 심리 상담사입니다. " +
                "사용자의 이야기를 경청하고, 적절한 질문을 통해 더 깊은 대화를 이끌어내세요. " +
                "응답은 2-3문장으로 간결하게, 자연스러운 대화체로 작성하세요.";

        switch (mood) {
            case VERY_SAD:
            case SAD:
                return basePrompt + "\n\n사용자가 힘든 시간을 보내고 있습니다. " +
                        "충분히 공감하고 위로하되, 감정을 억누르지 말고 표현하도록 격려하세요. " +
                        "가볍게 넘기지 말고 진지하게 경청하는 태도를 보여주세요.";
            
            case NEUTRAL:
                return basePrompt + "\n\n사용자의 평범한 일상에도 의미를 찾아주고, " +
                        "더 깊은 감정이나 생각이 있는지 자연스럽게 물어보세요.";
            
            case HAPPY:
            case VERY_HAPPY:
                return basePrompt + "\n\n사용자의 기쁨을 함께 축하하고, " +
                        "무엇이 행복하게 만들었는지 더 자세히 이야기하도록 유도하세요.";
            
            default:
                return basePrompt;
        }
    }

    /**
     * 일반 상담사 프롬프트 (감정 정보가 없을 때)
     */
    private String buildGeneralCounselorPrompt() {
        return "당신은 따뜻하고 공감 능력이 뛰어난 심리 상담사입니다. " +
                "사용자의 이야기를 경청하고, 적절한 질문을 통해 더 깊은 대화를 이끌어내세요. " +
                "응답은 2-3문장으로 간결하게, 자연스러운 대화체로 작성하세요.";
    }
}
