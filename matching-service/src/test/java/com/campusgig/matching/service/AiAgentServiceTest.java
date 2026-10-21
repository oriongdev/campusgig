package com.campusgig.matching.service;

import com.campusgig.matching.domain.AiRecommendation;
import com.campusgig.matching.domain.AiRecommendationResult;
import com.campusgig.matching.domain.JobAdReview;
import com.campusgig.matching.domain.JobRecommendation;
import com.campusgig.matching.domain.JobSnapshot;
import com.campusgig.matching.domain.StudentSnapshot;
import com.campusgig.matching.infrastructure.ai.JobAdAssistant;
import com.campusgig.matching.infrastructure.ai.JobAdCritic;
import com.campusgig.matching.infrastructure.ai.JobMatchingAgent;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import com.campusgig.matching.infrastructure.persistence.StudentSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAgentServiceTest {

    private static final String GOOD_REWRITE = """
            {"improvedTitle": "Weekend Barista", "improvedDescription": "Make great coffee for $28.50 an hour.",
             "tips": ["The draft had no detail about the role."]}
            """;
    private static final String APPROVED = "{\"approved\": true, \"issues\": []}";

    private final JobMatchingAgent matchingAgent = mock(JobMatchingAgent.class);
    private final JobAdAssistant assistant = mock(JobAdAssistant.class);
    private final JobAdCritic critic = mock(JobAdCritic.class);
    private final StudentSnapshotRepository students = mock(StudentSnapshotRepository.class);
    private final JobSnapshotRepository jobs = mock(JobSnapshotRepository.class);
    private final MatchingService ruleBased = mock(MatchingService.class);

    private AiAgentService service;

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> providerOf(T bean) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(bean);
        return provider;
    }

    private AiAgentService serviceWith(JobMatchingAgent agent, JobAdAssistant writer, JobAdCritic reviewer, int rounds) {
        return new AiAgentService(providerOf(agent), providerOf(writer), providerOf(reviewer),
                students, jobs, ruleBased, rounds);
    }

    @BeforeEach
    void setUp() {
        service = serviceWith(matchingAgent, assistant, critic, 1);

        StudentSnapshot alex = new StudentSnapshot(1L);
        alex.update("Alex Nguyen", Set.of("barista"), "WEEKENDS");
        when(students.findById(1L)).thenReturn(Optional.of(alex));
        when(jobs.findById(anyLong())).thenReturn(Optional.empty());
        when(jobs.findById(1L)).thenReturn(Optional.of(
                new JobSnapshot(1L, "Weekend Barista", "Make coffee", "hospitality", "Wollongong")));
        when(jobs.findById(2L)).thenReturn(Optional.of(
                new JobSnapshot(2L, "Java Tutor", "Tutor first years", "tutoring", "UOW Campus")));
    }

    // ------------------------------------------------------------ job matching agent

    @Test
    void withoutAnLlmTheAiUseCasesReportThatAiIsNotConfigured() {
        AiAgentService noAi = serviceWith(null, null, null, 1);

        assertFalse(noAi.isEnabled());
        assertThrows(AiUnavailableException.class, () -> noAi.recommendJobs(1L));
        assertThrows(AiUnavailableException.class, () -> noAi.reviewJobAd("t", "c", "l", "p", "d"));
    }

    @Test
    void theStudentsProfileIsSentToTheAgent() {
        when(matchingAgent.recommendJobs(anyString())).thenReturn("[]");

        service.recommendJobs(1L);

        verify(matchingAgent).recommendJobs("name: Alex Nguyen; skills: barista; availability: WEEKENDS");
    }

    @Test
    void anUnknownStudentIsReportedBeforeAnyLlmCallIsMade() {
        when(students.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.recommendJobs(99L));
        verify(matchingAgent, never()).recommendJobs(anyString());
    }

    @Test
    void agentRecommendationsAreParsedEvenWhenWrappedInMarkdownFences() {
        when(matchingAgent.recommendJobs(anyString())).thenReturn("""
                Here are the best matches:
                ```json
                [{"jobId": 2, "score": 0.4, "reason": "Could tutor"},
                 {"jobId": 1, "score": 0.9, "reason": "Barista skills and weekend availability"}]
                ```
                """);

        AiRecommendationResult result = service.recommendJobs(1L);

        assertEquals(AiRecommendationResult.SOURCE_AI_AGENT, result.source());
        assertEquals(List.of(1L, 2L), result.recommendations().stream().map(AiRecommendation::jobId).toList(),
                "best score first");
        assertEquals("Weekend Barista", result.recommendations().get(0).title(),
                "the title comes from our own data, not from the model");
    }

    @Test
    void jobsTheAgentInventedAreDroppedAndScoresAreKeptBetweenZeroAndOne() {
        when(matchingAgent.recommendJobs(anyString())).thenReturn("""
                [{"jobId": 77, "score": 1.0, "reason": "A job that does not exist"},
                 {"jobId": 1, "score": 7, "reason": "Too keen"},
                 {"jobId": 2, "score": -3, "reason": "Too gloomy"},
                 {"jobId": 1, "score": 0.2, "reason": "Duplicate"}]
                """);

        List<AiRecommendation> recommendations = service.recommendJobs(1L).recommendations();

        assertEquals(2, recommendations.size());
        assertEquals(1.0, recommendations.get(0).score());
        assertEquals("Too keen", recommendations.get(0).reason());
        assertEquals(0.0, recommendations.get(1).score());
    }

    @Test
    void ifTheLlmCallFailsTheRuleBasedMatcherAnswersInstead() {
        when(matchingAgent.recommendJobs(anyString())).thenThrow(new IllegalStateException("429 quota exceeded"));
        when(ruleBased.recommendJobs(1L)).thenReturn(List.of(
                new JobRecommendation(1L, "Weekend Barista", "hospitality", 1.0, "Matches skills: barista")));

        AiRecommendationResult result = service.recommendJobs(1L);

        assertEquals(AiRecommendationResult.SOURCE_RULE_BASED_FALLBACK, result.source());
        assertTrue(result.note().contains("429 quota exceeded"));
        assertEquals("Matches skills: barista", result.recommendations().get(0).reason());
    }

    @Test
    void ifTheAgentRepliesWithProseTheRuleBasedMatcherAnswersInstead() {
        when(matchingAgent.recommendJobs(anyString())).thenReturn("Sorry, I cannot help with that.");
        when(ruleBased.recommendJobs(1L)).thenReturn(List.of());

        AiRecommendationResult result = service.recommendJobs(1L);

        assertEquals(AiRecommendationResult.SOURCE_RULE_BASED_FALLBACK, result.source());
        assertTrue(result.note().contains("unexpected format"));
        assertTrue(result.recommendations().isEmpty());
    }

    // ------------------------------------------------------------ job ad assistant

    private JobAdReview reviewDraft() {
        return service.reviewJobAd("barista", "hospitality", "Wollongong", "28.50 HOURLY", "make coffee");
    }

    @Test
    void anApprovedRewriteIsReturnedWithoutRevision() {
        when(assistant.improve(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(GOOD_REWRITE);
        when(critic.review(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(APPROVED);

        JobAdReview review = reviewDraft();

        assertEquals("Weekend Barista", review.improvedTitle());
        assertEquals(List.of("The draft had no detail about the role."), review.tips());
        assertEquals(0, review.revisions());
        assertTrue(review.reviewNotes().isEmpty());
        verify(assistant, never()).revise(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void whenTheCriticFindsAProblemTheAssistantRevisesItsRewrite() {
        when(assistant.improve(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("""
                {"improvedTitle": "Weekend Barista", "improvedDescription": "Make coffee. Free staff meals!", "tips": []}
                """);
        when(critic.review(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{\"approved\": false, \"issues\": [\"Free staff meals are not in the draft.\"]}");
        when(assistant.revise(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(GOOD_REWRITE);

        JobAdReview review = reviewDraft();

        assertEquals(1, review.revisions());
        assertEquals(List.of("Free staff meals are not in the draft."), review.reviewNotes());
        assertEquals("Make great coffee for $28.50 an hour.", review.improvedDescription());
        verify(assistant).revise(eq("barista"), eq("hospitality"), eq("Wollongong"), eq("28.50 HOURLY"), eq("make coffee"),
                eq("Weekend Barista"), eq("Make coffee. Free staff meals!"),
                eq("- Free staff meals are not in the draft."));
    }

    @Test
    void aChangedPayAmountIsCaughtByTheRuleBasedGuardEvenIfTheCriticApproves() {
        when(assistant.improve(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("""
                {"improvedTitle": "Weekend Barista", "improvedDescription": "Earn $35 an hour.", "tips": []}
                """);
        when(critic.review(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(APPROVED);
        when(assistant.revise(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(GOOD_REWRITE);

        JobAdReview review = reviewDraft();

        assertEquals(1, review.revisions());
        assertTrue(review.reviewNotes().get(0).contains("$35"));
        verify(assistant).revise(any(), any(), any(), any(), any(), any(), any(), contains("$35"));
    }

    @Test
    void theReviewIsBestEffortSoACriticFailureKeepsTheFirstRewrite() {
        when(assistant.improve(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(GOOD_REWRITE);
        when(critic.review(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("timeout"));

        JobAdReview review = reviewDraft();

        assertEquals("Weekend Barista", review.improvedTitle());
        assertEquals(0, review.revisions());
    }

    @Test
    void anUnreadableVerdictCountsAsApproved() {
        when(assistant.improve(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(GOOD_REWRITE);
        when(critic.review(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("Looks good to me!");

        assertEquals(0, reviewDraft().revisions());
    }

    @Test
    void ifTheRevisionIsUnusableTheEmployerStillGetsTheFirstRewriteAndTheReviewNotes() {
        when(assistant.improve(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(GOOD_REWRITE);
        when(critic.review(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{\"approved\": false, \"issues\": [\"Tone is too casual.\"]}");
        when(assistant.revise(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn("not json");

        JobAdReview review = reviewDraft();

        assertEquals("Weekend Barista", review.improvedTitle());
        assertEquals(0, review.revisions());
        assertEquals(List.of("Tone is too casual."), review.reviewNotes());
    }

    @Test
    void reviewRoundsCanBeSwitchedOff() {
        AiAgentService noReview = serviceWith(matchingAgent, assistant, critic, 0);
        when(assistant.improve(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(GOOD_REWRITE);

        noReview.reviewJobAd("barista", "hospitality", "Wollongong", "28.50 HOURLY", "make coffee");

        verify(critic, never()).review(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void theCriticReviewsEachRevisionWhenMoreRoundsAreAllowed() {
        AiAgentService twoRounds = serviceWith(matchingAgent, assistant, critic, 2);
        when(assistant.improve(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(GOOD_REWRITE);
        when(critic.review(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{\"approved\": false, \"issues\": [\"Too long.\"]}")
                .thenReturn(APPROVED);
        when(assistant.revise(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(GOOD_REWRITE);

        JobAdReview review = twoRounds.reviewJobAd("barista", "hospitality", "Wollongong", "28.50 HOURLY", "make coffee");

        assertEquals(1, review.revisions());
        verify(critic, times(2)).review(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void aFailedFirstRewriteIsAnErrorBecauseThereIsNothingToFallBackTo() {
        when(assistant.improve(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("connection refused"));

        AiException error = assertThrows(AiException.class, this::reviewDraft);

        assertTrue(error.getMessage().contains("connection refused"));
    }

    @Test
    void aFirstRewriteInTheWrongFormatIsAnError() {
        when(assistant.improve(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("I would love to help you write this ad.");

        assertThrows(AiException.class, this::reviewDraft);
    }

    @Test
    void missingFieldsInTheRewriteFallBackToTheEmployersOwnText() {
        when(assistant.improve(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{\"improvedDescription\": \"Make great coffee.\"}");
        when(critic.review(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(APPROVED);

        JobAdReview review = reviewDraft();

        assertEquals("barista", review.improvedTitle());
        assertTrue(review.tips().isEmpty());
    }

    @Test
    void apiKeysNeverLeakIntoErrorMessages() {
        when(matchingAgent.recommendJobs(anyString()))
                .thenThrow(new IllegalStateException("GET https://example.test/v1?key=SECRET123 failed"));
        when(ruleBased.recommendJobs(1L)).thenReturn(List.of());

        String note = service.recommendJobs(1L).note();

        assertFalse(note.contains("SECRET123"));
        assertTrue(note.contains("key=***"));
    }
}
