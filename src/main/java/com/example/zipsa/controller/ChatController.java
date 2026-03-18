package com.example.zipsa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final VertexAiGeminiChatModel chatModel;

    private final ChatClient chatClient;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        return chatClient.prompt()
                .user(request.message())
                .stream()
                .content()
                .onErrorResume(Exception.class, e -> {
                    log.error("Vertex AI Gemini API error: {}", e.getMessage(), e);
                    return Flux.just("[Error] " + e.getMessage());
                });
    }

    public record ChatRequest(String message) {}
}
