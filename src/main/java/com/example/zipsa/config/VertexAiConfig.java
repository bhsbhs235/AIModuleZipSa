package com.example.zipsa.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.ai.vertexai.embedding.VertexAiEmbeddingConnectionDetails;
import org.springframework.ai.vertexai.embedding.text.VertexAiTextEmbeddingModel;
import org.springframework.ai.vertexai.embedding.text.VertexAiTextEmbeddingOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.zipsa.tool.LawServiceTool;
import com.google.cloud.vertexai.VertexAI;

@Configuration
public class VertexAiConfig {

    @Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;

    @Value("${spring.ai.vertex.ai.gemini.location}")
    private String location;

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model}")
    private String model;

    @Value("${spring.ai.vertex.ai.embedding.project-id}")
    private String embeddingProjectId;

    @Value("${spring.ai.vertex.ai.embedding.location}")
    private String embeddingLocation;

    @Value("${spring.ai.vertex.ai.embedding.text.options.model}")
    private String embeddingModel;

    @Bean
    public VertexAI vertexAI() {
        return new VertexAI.Builder()
                .setProjectId(projectId)
                .setLocation(location)
                .build();
    }

    @Bean
    public VertexAiGeminiChatModel vertexAiGeminiChatModel(VertexAI vertexAI) {
        VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                .model(model)
                .temperature(0.7)
                .build();

        return VertexAiGeminiChatModel.builder()
                .vertexAI(vertexAI)
                .defaultOptions(options)
                .build();
    }

    @Bean
    public VertexAiEmbeddingConnectionDetails embeddingConnectionDetails() {
        return VertexAiEmbeddingConnectionDetails.builder()
                .projectId(embeddingProjectId)
                .location(embeddingLocation)
                .build();
    }

    @Bean
    public VertexAiTextEmbeddingModel vertexAiTextEmbeddingModel(VertexAiEmbeddingConnectionDetails connectionDetails) {
        VertexAiTextEmbeddingOptions options = VertexAiTextEmbeddingOptions.builder()
                .model(embeddingModel)
                .build();

        return new VertexAiTextEmbeddingModel(connectionDetails, options);
    }

    @Bean
    @Qualifier("vertexAiChatClient")
    public ChatClient vertexAiChatClient(VertexAiGeminiChatModel chatModel, LawServiceTool lawServiceTool) {
        String systemPrompt = """
                당신은 '집사'라는 이름의 부동산 AI 에이전트입니다.

                역할:
                - 사용자의 부동산 관련 질문에 친절하고 정확하게 답변합니다.
                - 매물 검색, 시세 조회, 실거래가 분석, 지역 정보 제공 등을 수행합니다.
                - 전문 용어는 쉽게 풀어서 설명합니다.

                규칙:
                - 항상 한국어로 답변합니다.
                - 부동산과 무관한 질문에는 정중히 부동산 관련 질문을 유도합니다.
                - 답변은 간결하되 핵심 정보를 빠뜨리지 않습니다.
                """;

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(lawServiceTool)
                .build();
    }
}
