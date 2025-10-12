package Wonbin.FinalProject.ai.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {


    private final WebClient webClient;

    public OpenAIService(@Value("${openai.api-key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // 대화할 때 필요한 프롬프트 및 AI 특성 조작 필요. 특히 우울감 있는 환자와 대화할 때 주의해야 할 것들 인지시킬 필요 있음
    // 대화 (저비용 모델)
    public Mono<String> chat(String prompt) {
        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    return (String) msg.get("content");
                });
    }


    // 요약 (상위 모델)
    public Mono<String> summarize(String text) {
        Map<String, Object> body = Map.of(
                "model", "gpt-4o",
                "messages", List.of(
                        Map.of("role", "system", "content", "다음 대화를 하루 일기 형식으로 요약해줘."),
                        Map.of("role", "user", "content", text)
                )
        );

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    return (String) msg.get("content");
                });
    }

    /**
     * 🔥 텍스트를 벡터로 변환 (Embedding)
     * 텍스트의 의미를 1536개 숫자 배열로 표현
     */
    public float[] createEmbedding(String text) {
        Map<String, Object> body = Map.of(
                "model", "text-embedding-3-small",  // 가장 저렴한 모델
                "input", text
        );

        Map<String, Object> response = webClient.post()
                .uri("/embeddings")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // 응답에서 벡터 추출
        List<Map<String, Object>> data = 
            (List<Map<String, Object>>) response.get("data");
        List<Double> embedding = 
            (List<Double>) data.get(0).get("embedding");

        // float 배열로 변환
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }
}
