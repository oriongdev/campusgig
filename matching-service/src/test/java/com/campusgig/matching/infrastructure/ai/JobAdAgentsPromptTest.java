package com.campusgig.matching.infrastructure.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the prompt templates of the writer and critic agents render correctly.
 * A misspelt template variable only fails when the agent is called, so it is worth a test.
 */
class JobAdAgentsPromptTest {

    private static String systemPrompt(ScriptedChatModel model) {
        return assertInstanceOf(SystemMessage.class, model.requests.get(0).messages().get(0)).text();
    }

    private static String userPrompt(ScriptedChatModel model) {
        List<ChatMessage> messages = model.requests.get(0).messages();
        return assertInstanceOf(UserMessage.class, messages.get(messages.size() - 1)).singleText();
    }

    @Test
    void improveSendsTheDraftAndTheRules() {
        ScriptedChatModel model = new ScriptedChatModel(AiMessage.from("{}"));
        JobAdAssistant assistant = AiServices.builder(JobAdAssistant.class).chatModel(model).build();

        String reply = assistant.improve("Barista", "hospitality", "Wollongong", "28.50 HOURLY", "make coffee");

        assertEquals("{}", reply);
        assertTrue(systemPrompt(model).contains("never change the pay, location or category"));
        String user = userPrompt(model);
        assertTrue(user.contains("Title: Barista"));
        assertTrue(user.contains("Pay: 28.50 HOURLY"));
        assertTrue(user.contains("Description: make coffee"));
    }

    @Test
    void reviseSendsTheOriginalTheRewriteAndTheReviewersIssues() {
        ScriptedChatModel model = new ScriptedChatModel(AiMessage.from("{}"));
        JobAdAssistant assistant = AiServices.builder(JobAdAssistant.class).chatModel(model).build();

        assistant.revise("Barista", "hospitality", "Wollongong", "28.50 HOURLY", "make coffee",
                "Star Barista", "Earn $35 an hour", "- The pay was changed to $35.");

        assertTrue(systemPrompt(model).contains("Reply with JSON only"));
        String user = userPrompt(model);
        assertTrue(user.contains("Pay: 28.50 HOURLY"));
        assertTrue(user.contains("Title: Star Barista"));
        assertTrue(user.contains("- The pay was changed to $35."));
        assertTrue(user.contains("fix every problem"));
    }

    @Test
    void theCriticSeesBothVersionsAndItsOwnInstructions() {
        ScriptedChatModel model = new ScriptedChatModel(AiMessage.from("{\"approved\": true, \"issues\": []}"));
        JobAdCritic critic = AiServices.builder(JobAdCritic.class).chatModel(model).build();

        String verdict = critic.review("Barista", "hospitality", "Wollongong", "28.50 HOURLY", "make coffee",
                "Star Barista", "Make great coffee at our Wollongong cafe.");

        assertTrue(verdict.contains("\"approved\": true"));
        assertTrue(systemPrompt(model).contains("careful reviewer"));
        String user = userPrompt(model);
        assertTrue(user.contains("Description: make coffee"));
        assertTrue(user.contains("Description: Make great coffee at our Wollongong cafe."));
    }
}
