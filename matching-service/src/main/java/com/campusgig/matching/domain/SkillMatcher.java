package com.campusgig.matching.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Domain service: scores how well a job fits a student, based on the student's skills.
 * This is the rule-based first version; the LangChain4j matching agent will build on it.
 */
public class SkillMatcher {

    public Optional<JobRecommendation> match(StudentSnapshot student, JobSnapshot job) {
        Set<String> skills = student.getSkills();
        if (skills.isEmpty()) {
            return Optional.empty();
        }
        String jobText = String.join(" ",
                Objects.toString(job.getTitle(), ""),
                Objects.toString(job.getCategory(), ""),
                Objects.toString(job.getDescription(), "")).toLowerCase();

        List<String> matched = skills.stream()
                .filter(jobText::contains)
                .sorted()
                .toList();
        if (matched.isEmpty()) {
            return Optional.empty();
        }
        double score = Math.round(100.0 * matched.size() / skills.size()) / 100.0;
        return Optional.of(new JobRecommendation(
                job.getJobId(),
                job.getTitle(),
                job.getCategory(),
                score,
                "Matches skills: " + String.join(", ", matched)));
    }
}
