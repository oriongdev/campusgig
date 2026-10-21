package com.campusgig.matching.infrastructure.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Wires the LangChain4j agents to Google Gemini.
 * Only active when an API key is configured, so the rest of the service still runs without one.
 */
@Configuration
@ConditionalOnExpression("'${campusgig.ai.api-key:}'.trim() != ''")
public class GeminiConfig {

    private static final Logger log = LoggerFactory.getLogger(GeminiConfig.class);

    /** Stops the matching agent calling tools forever if the model gets stuck in a loop. */
    private static final int MAX_TOOL_ROUND_TRIPS = 5;

    @Bean
    public ChatModel geminiChatModel(@Value("${campusgig.ai.api-key}") String apiKey,
                                     @Value("${campusgig.ai.model}") String modelName,
                                     @Value("${campusgig.ai.timeout-seconds:60}") long timeoutSeconds) {
        log.info("AI agents enabled using Gemini model {}", modelName);
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey.trim())
                .modelName(modelName)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(2)
                // Gemini 3 models require the thought signature to be echoed back with each
                // function call, so thinking must be captured and resent or tool calls fail with 400.
                .returnThinking(true)
                .sendThinking(true)
                .build();
    }

    @Bean
    public JobMatchingAgent jobMatchingAgent(ChatModel chatModel, JobCatalogTools tools) {
        return AiServices.builder(JobMatchingAgent.class)
                .chatModel(chatModel)
                .tools(tools)
                .maxToolCallingRoundTrips(MAX_TOOL_ROUND_TRIPS)
                .build();
    }

    @Bean
    public JobAdAssistant jobAdAssistant(ChatModel chatModel) {
        return AiServices.builder(JobAdAssistant.class)
                .chatModel(chatModel)
                .build();
    }

    @Bean
    public JobAdCritic jobAdCritic(ChatModel chatModel) {
        return AiServices.builder(JobAdCritic.class)
                .chatModel(chatModel)
                .build();
    }
}
