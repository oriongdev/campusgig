package com.campusgig.matching.infrastructure.ai;

import com.campusgig.matching.domain.JobSnapshot;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the tool-use pattern end to end with a scripted model in place of Gemini:
 * the agent is offered the tools, the model asks for one, LangChain4j runs it against the job
 * catalogue, and the result is sent back to the model before it answers.
 */
class JobMatchingAgentToolUseTest {

    private final JobSnapshotRepository jobs = mock(JobSnapshotRepository.class);

    private JobMatchingAgent agentBackedBy(ScriptedChatModel model) {
        return AiServices.builder(JobMatchingAgent.class)
                .chatModel(model)
                .tools(new JobCatalogTools(jobs))
                .build();
    }

    private static ToolExecutionRequest call(String tool, String arguments) {
        return ToolExecutionRequest.builder().id("call-1").name(tool).arguments(arguments).build();
    }

    @Test
    void theAgentLooksUpJobsWithAToolBeforeAnswering() {
        when(jobs.findAll()).thenReturn(List.of(
                new JobSnapshot(1L, "Weekend Barista", "Make coffee", "hospitality", "Wollongong")));
        ScriptedChatModel model = new ScriptedChatModel(
                AiMessage.from(call("listOpenJobs", "{}")),
                AiMessage.from("[{\"jobId\": 1, \"score\": 0.9, \"reason\": \"Barista skills match\"}]"));

        String answer = agentBackedBy(model).recommendJobs("name: Alex; skills: barista; availability: WEEKENDS");

        assertEquals(2, model.requests.size(), "one request to pick a tool, one to answer with its result");

        // Request 1: the system prompt, the student's profile and both tools are sent to the model.
        List<ChatMessage> first = model.requests.get(0).messages();
        assertInstanceOf(SystemMessage.class, first.get(0));
        assertTrue(((SystemMessage) first.get(0)).text().contains("Never invent a job"));
        assertTrue(((UserMessage) first.get(1)).singleText().contains("skills: barista"));
        Set<String> offeredTools = model.requests.get(0).toolSpecifications().stream()
                .map(ToolSpecification::name)
                .collect(Collectors.toSet());
        assertEquals(Set.of("listOpenJobs", "findJobsByCategory"), offeredTools);

        // Request 2: the tool really ran, and its output went back to the model.
        verify(jobs).findAll();
        List<ChatMessage> second = model.requests.get(1).messages();
        ToolExecutionResultMessage toolResult =
                assertInstanceOf(ToolExecutionResultMessage.class, second.get(second.size() - 1));
        assertEquals("listOpenJobs", toolResult.toolName());
        assertTrue(toolResult.text().contains("jobId=1 | title=Weekend Barista"));

        assertTrue(answer.contains("\"jobId\": 1"));
    }

    @Test
    void theAgentCanFilterByCategory() {
        when(jobs.findAll()).thenReturn(List.of(
                new JobSnapshot(1L, "Weekend Barista", "Make coffee", "hospitality", "Wollongong"),
                new JobSnapshot(2L, "Java Tutor", "Tutor first years", "tutoring", "UOW Campus")));
        ScriptedChatModel model = new ScriptedChatModel(
                AiMessage.from(call("findJobsByCategory", "{\"category\": \"Tutoring\"}")),
                AiMessage.from("[]"));

        agentBackedBy(model).recommendJobs("name: Sam; skills: java; availability: EVENINGS");

        List<ChatMessage> second = model.requests.get(1).messages();
        ToolExecutionResultMessage toolResult =
                assertInstanceOf(ToolExecutionResultMessage.class, second.get(second.size() - 1));
        assertTrue(toolResult.text().contains("Java Tutor"));
        assertTrue(!toolResult.text().contains("Weekend Barista"), "only the requested category is returned");
    }
}
