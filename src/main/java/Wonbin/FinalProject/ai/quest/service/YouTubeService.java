package Wonbin.FinalProject.ai.quest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class YouTubeService {

    private final WebClient webClient;
    private final String apiKey;

    public YouTubeService(@Value("${youtube.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://www.googleapis.com/youtube/v3")
                .build();
    }

    /**
     * 검색어로 유튜브 영상 검색 (가장 관련성 높은 영상 1개)
     */
    public String searchVideo(String query) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", query)
                            .queryParam("type", "video")
                            .queryParam("maxResults", 1)
                            .queryParam("order", "relevance")
                            .queryParam("key", apiKey)
                            .queryParam("regionCode", "KR")
                            .queryParam("relevanceLanguage", "ko")
                            .queryParam("videoDuration", "short")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items != null && !items.isEmpty()) {
                Map<String, Object> id = (Map<String, Object>) items.get(0).get("id");
                String videoId = (String) id.get("videoId");
                return "https://www.youtube.com/watch?v=" + videoId;
            }
        } catch (Exception e) {
            System.err.println("YouTube 검색 실패: " + e.getMessage());
        }
        return null;
    }
}
