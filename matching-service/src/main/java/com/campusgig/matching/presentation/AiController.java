package com.campusgig.matching.presentation;

import com.campusgig.matching.domain.AiRecommendationResult;
import com.campusgig.matching.domain.JobAdReview;
import com.campusgig.matching.presentation.dto.JobAdRequest;
import com.campusgig.matching.service.AiAgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAgentService aiAgentService;

    public AiController(AiAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    /** Tells the UI whether an LLM is configured. */
    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("enabled", aiAgentService.isEnabled());
    }

    /** Job matching agent (requirement 13). Falls back to rule-based matches if the LLM fails. */
    @GetMapping("/students/{studentId}/recommendations")
    public AiRecommendationResult recommendations(@PathVariable("studentId") Long studentId) {
        return aiAgentService.recommendJobs(studentId);
    }

    /** Job ad assistant with a reviewer agent (requirement 14). */
    @PostMapping("/job-ad/review")
    public JobAdReview reviewJobAd(@RequestBody JobAdRequest request) {
        return aiAgentService.reviewJobAd(
                request.title(), request.category(), request.location(), request.pay(), request.description());
    }
}
