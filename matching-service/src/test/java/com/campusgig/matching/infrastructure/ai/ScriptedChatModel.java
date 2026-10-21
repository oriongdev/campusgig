package com.campusgig.matching.infrastructure.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A stand-in for the LLM in tests. It replays a fixed list of replies and records every request,
 * so the agents can be tested through real LangChain4j AiServices without calling Gemini.
 */
class ScriptedChatModel implements ChatModel {

    final List<ChatRequest> requests = new ArrayList<>();
    private final Deque<AiMessage> replies = new ArrayDeque<>();

    ScriptedChatModel(AiMessage... replies) {
        this.replies.addAll(List.of(replies));
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
        requests.add(request);
        if (replies.isEmpty()) {
            throw new IllegalStateException("The test script has no reply left for request " + requests.size());
        }
        return ChatResponse.builder().aiMessage(replies.removeFirst()).build();
    }
}
