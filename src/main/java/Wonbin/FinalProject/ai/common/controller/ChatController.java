package Wonbin.FinalProject.ai.common.controller;

import Wonbin.FinalProject.ai.common.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final OpenAIService openAIService;

    @GetMapping("/chat")
    public Mono<String> chat(@RequestParam String q) {
        return openAIService.chat(q);
    }

    @GetMapping("/summarize")
    public Mono<String> summarize(@RequestParam String text) {
        return openAIService.summarize(text);
    }
}
