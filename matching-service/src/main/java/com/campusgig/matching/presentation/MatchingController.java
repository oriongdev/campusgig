package com.campusgig.matching.presentation;

import com.campusgig.matching.domain.JobRecommendation;
import com.campusgig.matching.service.MatchingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/matching")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping("/students/{studentId}/recommendations")
    public List<JobRecommendation> recommendations(@PathVariable("studentId") Long studentId) {
        return matchingService.recommendJobs(studentId);
    }
}
