package com.campusgig.matching.service;

import com.campusgig.matching.domain.AiRecommendation;
import com.campusgig.matching.domain.JobAdReview;
import com.campusgig.matching.domain.JobSnapshot;
import com.campusgig.matching.domain.StudentSnapshot;
import com.campusgig.matching.infrastructure.ai.JobAdAssistant;
import com.campusgig.matching.infrastructure.ai.JobMatchingAgent;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import com.campusgig.matching.infrastructure.persistence.StudentSnapshotRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs the two agentic AI use cases and turns their answers into domain objects.
 */
@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    private final ObjectProvider<JobMatchingAgent> matchingAgent;
    private final ObjectProvider<JobAdAssistant> adAssistant;
    private final StudentSnapshotRepository students;
    private final JobSnapshotRepository jobs;
    private final ObjectMapper json = new ObjectMapper();

    public AiAgentService(ObjectProvider<JobMatchingAgent> matchingAgent,
                          ObjectProvider<JobAdAssistant> adAssistant,
                          StudentSnapshotRepository students,
                          JobSnapshotRepository jobs) {
        this.matchingAgent = matchingAgent;
        this.adAssistant = adAssistant;
        this.students = students;
        this.jobs = jobs;
    }

    public boolean isEnabled() {
        return matchingAgent.getIfAvailable() != null;
    }

    @Transactional(readOnly = true)
    public List<AiRecommendation> recommendJobs(Long studentId) {
        JobMatchingAgent agent = matchingAgent.getIfAvailable();
        if (agent == null) {
            throw new AiUnavailableException(
                    "AI is not configured. Set GEMINI_API_KEY and restart matching-service.");
        }
        StudentSnapshot student = students.findById(studentId)
                .orElseThrow(() -> new NotFoundException("No profile received yet for student " + studentId));

        String profile = "name: " + student.getName()
                + "; skills: " + String.join(", ", student.getSkills())
                + "; availability: " + student.getAvailability();

        String answer = agent.recommendJobs(profile);
        log.info("Matching agent replied for student {}", studentId);
        return toRecommendations(answer);
    }

    public JobAdReview reviewJobAd(String title, String category, String location, String pay, String description) {
        JobAdAssistant assistant = adAssistant.getIfAvailable();
        if (assistant == null) {
            throw new AiUnavailableException(
                    "AI is not configured. Set GEMINI_API_KEY and restart matching-service.");
        }
        String answer = assistant.improve(
                nullSafe(title), nullSafe(category), nullSafe(location), nullSafe(pay), nullSafe(description));
        try {
            Map<String, Object> parsed = json.readValue(stripFences(answer), new TypeReference<Map<String, Object>>() {});
            List<String> tips = new ArrayList<>();
            Object rawTips = parsed.get("tips");
            if (rawTips instanceof List<?> list) {
                list.forEach(tip -> tips.add(String.valueOf(tip)));
            }
            return new JobAdReview(
                    String.valueOf(parsed.getOrDefault("improvedTitle", title)),
                    String.valueOf(parsed.getOrDefault("improvedDescription", description)),
                    tips);
        } catch (Exception e) {
            throw new AiException("The AI replied in an unexpected format: " + preview(answer), e);
        }
    }

    private List<AiRecommendation> toRecommendations(String answer) {
        try {
            List<Map<String, Object>> parsed =
                    json.readValue(stripFences(answer), new TypeReference<List<Map<String, Object>>>() {});
            List<AiRecommendation> results = new ArrayList<>();
            for (Map<String, Object> entry : parsed) {
                Long jobId = entry.get("jobId") == null ? null : Long.valueOf(String.valueOf(entry.get("jobId")));
                Double score = entry.get("score") == null ? null : Double.valueOf(String.valueOf(entry.get("score")));
                String title = jobs.findById(jobId).map(JobSnapshot::getTitle).orElse("job #" + jobId);
                results.add(new AiRecommendation(jobId, title, score, String.valueOf(entry.get("reason"))));
            }
            return results;
        } catch (Exception e) {
            throw new AiException("The AI replied in an unexpected format: " + preview(answer), e);
        }
    }

    /** LLMs often wrap JSON in ``` fences even when asked not to. */
    private static String stripFences(String answer) {
        String cleaned = answer == null ? "" : answer.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "").trim();
        }
        return cleaned;
    }

    private static String preview(String answer) {
        if (answer == null) {
            return "(empty)";
        }
        return answer.length() > 200 ? answer.substring(0, 200) + "..." : answer;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
