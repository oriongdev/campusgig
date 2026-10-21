package com.campusgig.matching.presentation;

import com.campusgig.matching.domain.AiRecommendation;
import com.campusgig.matching.domain.AiRecommendationResult;
import com.campusgig.matching.domain.JobAdReview;
import com.campusgig.matching.service.AiAgentService;
import com.campusgig.matching.service.AiException;
import com.campusgig.matching.service.AiUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
class AiControllerTest {

    private static final String DRAFT = """
            {"title":"barista","category":"hospitality","location":"Wollongong","pay":"28.50 HOURLY","description":"make coffee"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiAgentService aiAgentService;

    @Test
    void statusTellsTheUiWhetherAnLlmIsConfigured() throws Exception {
        when(aiAgentService.isEnabled()).thenReturn(true);

        mockMvc.perform(get("/api/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void recommendationsSayWhereTheyCameFrom() throws Exception {
        when(aiAgentService.recommendJobs(1L)).thenReturn(AiRecommendationResult.fromAgent(List.of(
                new AiRecommendation(1L, "Weekend Barista", 0.9, "Barista skills and weekend availability"))));

        mockMvc.perform(get("/api/ai/students/1/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("AI_AGENT"))
                .andExpect(jsonPath("$.recommendations[0].jobId").value(1))
                .andExpect(jsonPath("$.recommendations[0].score").value(0.9));
    }

    @Test
    void aFallbackAnswerIsStillA200WithANote() throws Exception {
        when(aiAgentService.recommendJobs(1L)).thenReturn(
                AiRecommendationResult.fallback("The AI agent could not answer.", List.of()));

        mockMvc.perform(get("/api/ai/students/1/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("RULE_BASED_FALLBACK"))
                .andExpect(jsonPath("$.note").value("The AI agent could not answer."));
    }

    @Test
    void withoutAnApiKeyTheAgentsReturn503() throws Exception {
        when(aiAgentService.recommendJobs(1L)).thenThrow(new AiUnavailableException("AI is not configured."));

        mockMvc.perform(get("/api/ai/students/1/recommendations"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("AI is not configured."));
    }

    @Test
    void theJobAdReviewIncludesTheReviewersNotes() throws Exception {
        when(aiAgentService.reviewJobAd(any(), any(), any(), any(), any())).thenReturn(new JobAdReview(
                "Weekend Barista", "Make great coffee.", List.of("Too short."), 1, List.of("Pay was changed.")));

        mockMvc.perform(post("/api/ai/job-ad/review").contentType(MediaType.APPLICATION_JSON).content(DRAFT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.improvedTitle").value("Weekend Barista"))
                .andExpect(jsonPath("$.revisions").value(1))
                .andExpect(jsonPath("$.reviewNotes[0]").value("Pay was changed."));
    }

    @Test
    void anLlmFailureDuringAJobAdReviewReturns502NotARaw500() throws Exception {
        when(aiAgentService.reviewJobAd(any(), any(), any(), any(), any()))
                .thenThrow(new AiException("The AI assistant could not be reached: timeout", null));

        mockMvc.perform(post("/api/ai/job-ad/review").contentType(MediaType.APPLICATION_JSON).content(DRAFT))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("The AI assistant could not be reached: timeout"));
    }
}
