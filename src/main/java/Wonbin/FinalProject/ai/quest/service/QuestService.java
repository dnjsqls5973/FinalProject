package Wonbin.FinalProject.ai.quest.service;

import Wonbin.FinalProject.ai.common.service.OpenAIService;
import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.ai.quest.domain.Quest;
import Wonbin.FinalProject.ai.quest.domain.QuestCategory;
import Wonbin.FinalProject.ai.quest.domain.UserQuest;
import Wonbin.FinalProject.ai.quest.dto.QuestGenerationRequest;
import Wonbin.FinalProject.ai.quest.dto.QuestResponse;
import Wonbin.FinalProject.ai.quest.repository.QuestRepository;
import Wonbin.FinalProject.ai.quest.repository.UserQuestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestService {

    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;
    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper;
    private final Wonbin.FinalProject.auth.repository.UserRepository userRepository;
    private final YouTubeService youtubeService;

    /**
     * 오늘의 퀘스트 가져오기 (없으면 생성)
     */
    @Transactional
    public QuestResponse getTodayQuest(User user) {
        LocalDate today = LocalDate.now();

        // 1. 🔥 해당 사용자의 오늘 퀘스트가 이미 있는지 확인
        Quest quest = questRepository.findByUserAndQuestDate(user, today)
                .orElseGet(() -> generateAndSaveQuest(user, today));

        // 2. 사용자에게 할당된 퀘스트 조회 or 생성
        UserQuest userQuest = userQuestRepository.findByUserAndQuest(user, quest)
                .orElseGet(() -> createUserQuest(user, quest));

        return QuestResponse.from(quest, userQuest);
    }

    /**
     * 이메일로 오늘의 퀘스트 가져오기
     */
    @Transactional
    public QuestResponse getTodayQuestByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        return getTodayQuest(user);
    }

    /**
     * 🔥 비동기로 퀘스트 미리 생성 (백그라운드)
     */
    @Async
    @Transactional
    public CompletableFuture<Void> prepareQuestAsync(User user) {
        try {
            LocalDate today = LocalDate.now();
            
            // 🔥 해당 사용자의 오늘 퀘스트가 이미 있으면 스킵
            if (questRepository.existsByUserAndQuestDate(user, today)) {
                log.info("✅ Quest already exists for user {} today, skipping generation", user.getEmail());
                return CompletableFuture.completedFuture(null);
            }
            
            // 없으면 생성
            Quest quest = generateAndSaveQuest(user, today);
            createUserQuest(user, quest);
            
            log.info("✅ Quest pre-generated asynchronously for user: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("❌ Failed to pre-generate quest asynchronously for user: {}", user.getEmail(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * AI로 퀘스트 생성 후 저장 (중복 체크 포함)
     */
    private Quest generateAndSaveQuest(User user, LocalDate date) {
        log.info("📝 Generating new quest for user {} on date: {}", user.getEmail(), date);

        int maxAttempts = 3;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                // OpenAI로 퀘스트 생성
                String prompt = buildQuestPrompt(user, date);
                String aiResponse = openAIService.chat(prompt).block();

                // JSON 파싱
                QuestGenerationRequest questData = parseQuestResponse(aiResponse);

                // Quest 엔티티 생성
                Quest quest = Quest.builder()
                        .user(user)
                        .title(questData.getTitle())
                        .description(questData.getDescription())
                        .category(questData.getCategory())
                        .questDate(date)
                        .build();

                // 🔥 Embedding 생성 및 저장
                float[] embedding = openAIService.createEmbedding(quest.getTitle());
                quest.setTitleEmbeddingArray(embedding);

                // 🔥 HEALTH 카테고리면 유튜브 링크 추가
                if (quest.getCategory() == QuestCategory.HEALTH) {
                    String youtubeUrl = youtubeService.searchVideo(quest.getTitle());
                    if (youtubeUrl != null) {
                        quest.setYoutubeUrl(youtubeUrl);
                        log.info("✅ YouTube link added: {}", youtubeUrl);
                    }
                }

                // 🔥 중복 체크
                if (!isDuplicateQuest(user, quest)) {
                    log.info("✅ Unique quest generated: '{}'", quest.getTitle());
                    return questRepository.save(quest);
                }

                log.warn("⚠️ Duplicate quest detected (attempt {}/{}), regenerating...", 
                         attempt + 1, maxAttempts);

            } catch (Exception e) {
                log.error("❌ Failed to generate quest (attempt {}/{})", attempt + 1, maxAttempts, e);
            }
        }

        // 3번 실패하면 기본 퀘스트
        log.warn("❌ Failed to generate unique quest after {} attempts, using default", maxAttempts);
        return createDefaultQuest(user, date);
    }

    /**
     * 🔥 중복 퀘스트 체크 (Embedding 유사도 기반)
     */
    private boolean isDuplicateQuest(User user, Quest newQuest) {
        // 최근 30일 퀘스트 가져오기
        var recentQuests = questRepository
            .findByUserAndQuestDateBetween(
                user,
                LocalDate.now().minusDays(30),
                LocalDate.now().minusDays(1)
            );

        if (recentQuests.isEmpty()) {
            return false;  // 첫 퀘스트는 중복 없음
        }

        float[] newEmbedding = newQuest.getTitleEmbeddingArray();
        if (newEmbedding == null) {
            return false;  // Embedding 없으면 중복 체크 불가
        }

        for (Quest recent : recentQuests) {
            float[] recentEmbedding = recent.getTitleEmbeddingArray();

            // 기존 퀘스트에 embedding 없으면 생성
            if (recentEmbedding == null) {
                recentEmbedding = openAIService.createEmbedding(recent.getTitle());
                recent.setTitleEmbeddingArray(recentEmbedding);
                questRepository.save(recent);
            }

            // 🔥 코사인 유사도 계산
            double similarity = cosineSimilarity(newEmbedding, recentEmbedding);

            if (similarity > 0.85) {  // 85% 이상 유사하면 중복
                log.info("🔍 Similar quest found: '{}' ↔ '{}' (similarity: {:.2f}%)",
                         newQuest.getTitle(), recent.getTitle(), similarity * 100);
                return true;
            }
        }

        return false;
    }

    /**
     * 🔥 코사인 유사도 계산
     * 두 벡터 사이의 각도를 측정 (0~1, 1에 가까울수록 유사)
     */
    private double cosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA.length != vecB.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        // 1. 내적 계산 (A · B)
        double dotProduct = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
        }

        // 2. 크기 계산 (||A||, ||B||)
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }
        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        // 3. 코사인 유사도
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    /**
     * AI 프롬프트 생성 (우울증 환자 친화적 + 중복 방지)
     */
    private String buildQuestPrompt(User user, LocalDate date) {
        String dayOfWeek = date.getDayOfWeek().toString();

        // 최근 7일 퀘스트 가져오기
        var recentQuests = questRepository
            .findByUserAndQuestDateBetween(
                user,
                date.minusDays(7),
                date.minusDays(1)
            );

        String recentTitles = recentQuests.isEmpty()
            ? "없음 (처음 퀘스트입니다)"
            : recentQuests.stream()
                .map(q -> "- " + q.getTitle())
                .collect(java.util.stream.Collectors.joining("\n"));

        return String.format("""
                오늘은 %s (%s)입니다.
                
                사용자의 최근 7일 퀘스트:
                %s
                
                ⚠️ 중요: 위 퀘스트들과 완전히 다른 활동을 제안해주세요!
                - 다른 카테고리 우선
                - 다른 장소/시간/방식
                - 절대 비슷한 활동 금지
                
                우울증을 겪고 있는 사람이 하루 동안 부담 없이 완료할 수 있는 간단한 활동 1개를 제안해주세요.
                
                중요한 원칙:
                1. 10-15분 이내에 완료 가능한 활동
                2. 집에서 또는 가까운 곳에서 할 수 있는 것
                3. 실패해도 괜찮다는 따뜻하고 위로하는 톤
                4. "~해야 한다" 대신 "~해보는 건 어때요?" 같은 부드러운 제안
                5. 성취감을 느낄 수 있는 구체적인 활동
                6. 한국어로 작성
                7. 주어진 카테고리 안에서 순서 상관없이 랜덤으로 생성
                
                카테고리:
                1. MINDFULNESS (마음챙김): 깊게 숨쉬기, 밖으로 나가서 꽃이나 동물 사진 찍기, 나를 위한 한 마디 작성하기 등
                2. LEARNING (학습): 짧은 글 읽기, 새로운 단어 배우기 등
                3. SOCIAL (사회성): 가족/친구에게 짧은 메시지 보내기 등
                4. CREATIVE (창의성): 낙서하기(나만의 캐릭터 만들어보기), 좋아하는 음악 듣고 감상평 작성 등
                5. DAILY_LIFE (일상): 침대 정리, 설거지 하기, 내가 좋아하는 요리 직접 만들어보기, 따뜻한 물로 샤워하기 등
                6. HEALTH (건강): 유튜브 보고 5분 이상 스트레칭 해보기, 10분 이상 조깅, 명상하기 등
                
                예시:
                - 제목: "창문 열고 잠시 바깥 구경하기"
                - 설명: "잠깐이라도 괜찮아요. 좋아하는 노래와 함께 바깥 공기를 느끼며 세상을 구경해봐요. 그것만으로도 충분히 의미 있는 시간이에요."
                
                반드시 다음 JSON 형식으로만 응답해주세요:
                {
                  "title": "활동 제목 (20자 이내, 구체적으로)",
                  "description": "따뜻한 격려 메시지 + 구체적인 활동 제시 (80자 이내)",
                  "category": "HEALTH"
                }
                """, date, dayOfWeek, recentTitles);
    }

    /**
     * AI 응답 파싱
     */
    private QuestGenerationRequest parseQuestResponse(String aiResponse) throws Exception {
        // JSON 부분만 추출
        String jsonPart = aiResponse;
        
        if (aiResponse.contains("{")) {
            int start = aiResponse.indexOf("{");
            int end = aiResponse.lastIndexOf("}") + 1;
            jsonPart = aiResponse.substring(start, end);
        }

        return objectMapper.readValue(jsonPart, QuestGenerationRequest.class);
    }

    /**
     * 기본 퀘스트 생성 (AI 실패 시)
     */
    private Quest createDefaultQuest(User user, LocalDate date) {
        return questRepository.save(Quest.builder()
                .user(user)  // 🔥 사용자 연결
                .title("오늘 하루도 여기까지 온 나를 칭찬하기")
                .description("오늘 하루를 버텨낸 것만으로도 충분해요. " +
                           "스스로에게 \"잘하고 있어\"라고 한 번 말해주세요. " +
                           "작은 걸음도 전진이랍니다 💙")
                .category(QuestCategory.MINDFULNESS)
                .questDate(date)
                .build());
    }

    /**
     * UserQuest 생성 (사용자에게 퀘스트 할당)
     */
    private UserQuest createUserQuest(User user, Quest quest) {
        UserQuest userQuest = UserQuest.builder()
                .user(user)
                .quest(quest)
                .build();

        return userQuestRepository.save(userQuest);
    }

    /**
     * 퀘스트 완료 처리
     */
    @Transactional
    public void completeQuest(User user, Long questId) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new IllegalArgumentException("Quest not found"));

        UserQuest userQuest = userQuestRepository.findByUserAndQuest(user, quest)
                .orElseThrow(() -> new IllegalArgumentException("UserQuest not found"));

        userQuest.complete();
        log.info("✅ Quest completed: userId={}, questId={}", user.getId(), questId);
    }

    /**
     * 이메일로 퀘스트 완료 처리
     */
    @Transactional
    public void completeQuestByEmail(String email, Long questId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        completeQuest(user, questId);
    }
}
