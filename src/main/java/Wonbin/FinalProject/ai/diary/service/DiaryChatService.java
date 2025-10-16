package Wonbin.FinalProject.ai.diary.service;

import Wonbin.FinalProject.ai.common.service.OpenAIService;
import Wonbin.FinalProject.ai.diary.domain.Diary;
import Wonbin.FinalProject.ai.diary.domain.Mood;
import Wonbin.FinalProject.ai.diary.dto.ChatRequest;
import Wonbin.FinalProject.ai.diary.dto.ChatResponse;
import Wonbin.FinalProject.ai.diary.repository.DiaryRepository;
import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryChatService {

    private final OpenAIService openAIService;
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    // 사용자별 대화 컨텍스트 저장 (실제로는 Redis나 DB에 저장하는 것이 좋습니다)
    private final Map<Long, String> conversationContexts = new HashMap<>();

    /**
     * 일기 기반 대화 시작
     * 일기 내용과 감정을 기반으로 첫 AI 응답 생성
     */
    public ChatResponse startDiaryChat(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findByUserAndDiaryDate(user, date)
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 일기를 찾을 수 없습니다."));

        Mood mood = diary.getMood();
        String content = diary.getContent();

        // 시스템 프롬프트 생성 (상담사 느낌)
        String systemPrompt = buildCounselorPrompt(mood);
        
        // 일기 기반 첫 대화 시작
        String initialPrompt = String.format(
            "%s\n\n사용자의 일기:\n감정: %s\n내용: %s\n\n" +
            "위 일기를 읽고 공감하며 대화를 시작해주세요. 자연스럽게 질문을 던져서 사용자가 더 많이 이야기할 수 있도록 유도하세요.",
            systemPrompt, mood.getKey(), content
        );

        // OpenAI API 호출
        String aiResponse = openAIService.chat(initialPrompt).block();

        // 대화 컨텍스트 저장
        String context = String.format("일기 감정: %s\n일기 내용: %s\n대화 시작", mood.getKey(), content);
        conversationContexts.put(userId, context);

        log.info("일기 기반 대화 시작 - userId: {}, date: {}, mood: {}", userId, date, mood.getKey());

        return ChatResponse.builder()
                .message(aiResponse)
                .mood(mood.getKey())
                .build();
    }

    /**
     * 대화 계속하기
     */
    public ChatResponse continueChat(Long userId, ChatRequest request) {
        // 사용자의 컨텍스트 불러오기
        String context = conversationContexts.getOrDefault(userId, "");

        Mood mood = null;
        
        // 일기 날짜가 있으면 해당 일기의 감정 정보 가져오기
        if (request.getDiaryDate() != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            
            Diary diary = diaryRepository.findByUserAndDiaryDate(user, request.getDiaryDate())
                    .orElse(null);
            
            if (diary != null) {
                mood = diary.getMood();
            }
        }

        // 상담사 프롬프트
        String systemPrompt = mood != null 
            ? buildCounselorPrompt(mood) 
            : buildGeneralCounselorPrompt();

        // 대화 이어가기
        String fullPrompt = String.format(
            "%s\n\n이전 대화 맥락:\n%s\n\n사용자: %s\n\n자연스럽게 대화를 이어가며 공감하고, 필요하다면 적절한 질문을 던져주세요.",
            systemPrompt, context, request.getMessage()
        );

        // OpenAI API 호출
        String aiResponse = openAIService.chat(fullPrompt).block();

        // 대화 컨텍스트 업데이트
        String updatedContext = String.format("%s\n사용자: %s\nAI: %s", 
            context, request.getMessage(), aiResponse);
        conversationContexts.put(userId, updatedContext);

        log.info("대화 계속 - userId: {}", userId);

        return ChatResponse.builder()
                .message(aiResponse)
                .mood(mood != null ? mood.getKey() : null)
                .build();
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

    /**
     * 대화 컨텍스트 초기화 (선택적)
     */
    public void clearContext(Long userId) {
        conversationContexts.remove(userId);
        log.info("대화 컨텍스트 초기화 - userId: {}", userId);
    }
}
