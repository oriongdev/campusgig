package com.campusgig.matching.service;

import com.campusgig.matching.domain.AiRecommendation;
import com.campusgig.matching.domain.AiRecommendationResult;
import com.campusgig.matching.domain.JobAdGuard;
import com.campusgig.matching.domain.JobAdReview;
import com.campusgig.matching.domain.JobSnapshot;
import com.campusgig.matching.domain.StudentSnapshot;
import com.campusgig.matching.infrastructure.ai.JobAdAssistant;
import com.campusgig.matching.infrastructure.ai.JobAdCritic;
import com.campusgig.matching.infrastructure.ai.JobMatchingAgent;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import com.campusgig.matching.infrastructure.persistence.StudentSnapshotRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Runs the two agentic AI use cases and turns the agents' answers into domain objects.
 *
 * <p>An LLM is an unreliable collaborator: it can be offline, slow, over quota, or reply with
 * something that is not what was asked for. Nothing an agent returns is trusted until it has been
 * checked here, and each use case has a defined behaviour for when the agent fails.
 */
@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    static final int MAX_RECOMMENDATIONS = 5;
    private static final String NOT_CONFIGURED =
            "AI is not configured. Set GEMINI_API_KEY and restart matching-service.";

    private final ObjectProvider<JobMatchingAgent> matchingAgent;
    private final ObjectProvider<JobAdAssistant> adAssistant;
    private final ObjectProvider<JobAdCritic> adCritic;
    private final StudentSnapshotRepository students;
    private final JobSnapshotRepository jobs;
    private final MatchingService ruleBasedMatching;
    private final int adReviewRounds;
    private final JobAdGuard adGuard = new JobAdGuard();
    private final ObjectMapper json = new ObjectMapper();

    public AiAgentService(ObjectProvider<JobMatchingAgent> matchingAgent,
                          ObjectProvider<JobAdAssistant> adAssistant,
                          ObjectProvider<JobAdCritic> adCritic,
                          StudentSnapshotRepository students,
                          JobSnapshotRepository jobs,
                          MatchingService ruleBasedMatching,
                          @Value("${campusgig.ai.ad-review-rounds:1}") int adReviewRounds) {
        this.matchingAgent = matchingAgent;
        this.adAssistant = adAssistant;
        this.adCritic = adCritic;
        this.students = students;
        this.jobs = jobs;
        this.ruleBasedMatching = ruleBasedMatching;
        this.adReviewRounds = Math.max(0, adReviewRounds);
    }

    public boolean isEnabled() {
        return matchingAgent.getIfAvailable() != null;
    }

    // ---------------------------------------------------------------- job matching agent

    /**
     * Asks the matching agent which open jobs suit a student (requirement 13).
     * If the agent fails for any reason, the rule-based matcher answers instead.
     */
    public AiRecommendationResult recommendJobs(Long studentId) {
        JobMatchingAgent agent = matchingAgent.getIfAvailable();
        if (agent == null) {
            throw new AiUnavailableException(NOT_CONFIGURED);
        }
        StudentSnapshot student = students.findById(studentId)
                .orElseThrow(() -> new NotFoundException("No profile received yet for student " + studentId));
        String profile = "name: " + student.getName()
                + "; skills: " + String.join(", ", student.getSkills())
                + "; availability: " + student.getAvailability();
        try {
            List<AiRecommendation> recommendations = toRecommendations(agent.recommendJobs(profile));
            log.info("Matching agent returned {} recommendation(s) for student {}", recommendations.size(), studentId);
            return AiRecommendationResult.fromAgent(recommendations);
        } catch (RuntimeException e) {
            log.warn("Matching agent failed for student {}, using the rule-based matcher instead: {}",
                    studentId, describe(e));
            List<AiRecommendation> fallback = ruleBasedMatching.recommendJobs(studentId).stream()
                    .limit(MAX_RECOMMENDATIONS)
                    .map(match -> new AiRecommendation(match.jobId(), match.title(), match.score(), match.reason()))
                    .toList();
            return AiRecommendationResult.fallback(
                    "The AI agent could not answer (" + describe(e) + "). Showing rule-based matches instead.",
                    fallback);
        }
    }

    /**
     * Parses and validates the agent's answer. Jobs the agent made up are dropped, scores are
     * forced into the range 0 to 1, and at most {@link #MAX_RECOMMENDATIONS} are kept, best first.
     */
    private List<AiRecommendation> toRecommendations(String answer) {
        List<Map<String, Object>> parsed;
        try {
            parsed = json.readValue(extractJson(answer, '[', ']'), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new AiException("The AI replied in an unexpected format: " + preview(answer), e);
        }
        Map<Long, AiRecommendation> byJob = new LinkedHashMap<>();
        for (Map<String, Object> entry : parsed) {
            Long jobId = toLong(entry.get("jobId"));
            Optional<JobSnapshot> job = jobId == null ? Optional.empty() : jobs.findById(jobId);
            if (job.isEmpty()) {
                log.warn("Matching agent recommended job {} which is not an open job, ignoring it", entry.get("jobId"));
                continue;
            }
            Object reason = entry.get("reason");
            byJob.putIfAbsent(jobId, new AiRecommendation(
                    jobId,
                    job.get().getTitle(),
                    clampScore(entry.get("score")),
                    reason == null ? "" : shorten(String.valueOf(reason), 300)));
        }
        return byJob.values().stream()
                .sorted(Comparator.comparingDouble(AiRecommendation::score).reversed())
                .limit(MAX_RECOMMENDATIONS)
                .toList();
    }

    // ---------------------------------------------------------------- job ad assistant

    /**
     * Improves an employer's draft job ad (requirement 14) using a reflection loop:
     * the assistant rewrites the ad, a critic agent and a rule-based guard review the rewrite,
     * and the assistant revises it if they find problems.
     *
     * <p>The first rewrite must succeed. The review is best effort: if it fails, the employer
     * still gets the first rewrite.
     */
    public JobAdReview reviewJobAd(String title, String category, String location, String pay, String description) {
        JobAdAssistant assistant = adAssistant.getIfAvailable();
        if (assistant == null) {
            throw new AiUnavailableException(NOT_CONFIGURED);
        }
        Draft draft = new Draft(nullSafe(title), nullSafe(category), nullSafe(location), nullSafe(pay), nullSafe(description));

        String answer;
        try {
            answer = assistant.improve(draft.title, draft.category, draft.location, draft.pay, draft.description);
        } catch (RuntimeException e) {
            throw new AiException("The AI assistant could not be reached: " + describe(e), e);
        }
        Rewrite rewrite = toRewrite(answer, draft);

        List<String> reviewNotes = new ArrayList<>();
        int revisions = 0;
        JobAdCritic critic = adCritic.getIfAvailable();
        for (int round = 1; critic != null && round <= adReviewRounds; round++) {
            try {
                List<String> issues = review(critic, draft, rewrite);
                if (issues.isEmpty()) {
                    log.info("Job ad rewrite approved by the reviewer in round {}", round);
                    break;
                }
                log.info("Reviewer found {} problem(s) with the job ad rewrite in round {}: {}", issues.size(), round, issues);
                reviewNotes.addAll(issues);
                rewrite = toRewrite(assistant.revise(
                        draft.title, draft.category, draft.location, draft.pay, draft.description,
                        rewrite.title, rewrite.description, "- " + String.join("\n- ", issues)), draft);
                revisions++;
            } catch (RuntimeException e) {
                log.warn("Job ad review step failed, keeping the current rewrite: {}", describe(e));
                break;
            }
        }
        return new JobAdReview(rewrite.title, rewrite.description, rewrite.tips, revisions, List.copyOf(reviewNotes));
    }

    /** Problems found by the rule-based guard and by the critic agent, guard first. */
    private List<String> review(JobAdCritic critic, Draft draft, Rewrite rewrite) {
        List<String> issues = new ArrayList<>(adGuard.check(draft.allText(), rewrite.title, rewrite.description));
        String verdict = critic.review(draft.title, draft.category, draft.location, draft.pay, draft.description,
                rewrite.title, rewrite.description);
        try {
            Map<String, Object> parsed =
                    json.readValue(extractJson(verdict, '{', '}'), new TypeReference<Map<String, Object>>() {});
            boolean approved = Boolean.parseBoolean(String.valueOf(parsed.get("approved")));
            if (!approved && parsed.get("issues") instanceof List<?> list) {
                list.stream()
                        .map(String::valueOf)
                        .filter(issue -> !issue.isBlank())
                        .limit(5)
                        .forEach(issues::add);
            }
        } catch (Exception e) {
            log.warn("Could not read the reviewer's verdict, treating it as approved: {}", preview(verdict));
        }
        return issues;
    }

    private Rewrite toRewrite(String answer, Draft draft) {
        try {
            Map<String, Object> parsed =
                    json.readValue(extractJson(answer, '{', '}'), new TypeReference<Map<String, Object>>() {});
            List<String> tips = new ArrayList<>();
            if (parsed.get("tips") instanceof List<?> list) {
                list.stream().map(String::valueOf).limit(3).forEach(tips::add);
            }
            return new Rewrite(
                    textOr(parsed.get("improvedTitle"), draft.title),
                    textOr(parsed.get("improvedDescription"), draft.description),
                    tips);
        } catch (Exception e) {
            throw new AiException("The AI replied in an unexpected format: " + preview(answer), e);
        }
    }

    private record Draft(String title, String category, String location, String pay, String description) {
        String allText() {
            return String.join(" ", title, category, location, pay, description);
        }
    }

    private record Rewrite(String title, String description, List<String> tips) {
    }

    // ---------------------------------------------------------------- helpers

    /**
     * LLMs often wrap JSON in ``` fences or add a sentence around it, even when told not to.
     * This keeps only the text from the first opening bracket to the last closing bracket.
     */
    static String extractJson(String answer, char open, char close) {
        String text = answer == null ? "" : answer.trim();
        int start = text.indexOf(open);
        int end = text.lastIndexOf(close);
        return start >= 0 && end > start ? text.substring(start, end + 1) : text;
    }

    private static Long toLong(Object value) {
        try {
            return value == null ? null : Double.valueOf(String.valueOf(value)).longValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double clampScore(Object value) {
        try {
            double score = value == null ? 0.0 : Double.parseDouble(String.valueOf(value));
            if (Double.isNaN(score)) {
                return 0.0;
            }
            return Math.max(0.0, Math.min(1.0, score));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String textOr(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value).trim();
    }

    /** A one-line description of a failure that is safe to show to a user. */
    private static String describe(RuntimeException e) {
        if (e instanceof AiException) {
            return e.getMessage();
        }
        String message = e.getMessage() == null ? "" : e.getMessage()
                .replaceAll("(?i)key=[^&\\s\"]+", "key=***")
                .replaceAll("\\s+", " ")
                .trim();
        String name = e.getClass().getSimpleName();
        return message.isEmpty() ? name : name + ": " + shorten(message, 160);
    }

    private static String preview(String answer) {
        return answer == null || answer.isBlank() ? "(empty)" : shorten(answer, 200);
    }

    private static String shorten(String text, int max) {
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }
}
