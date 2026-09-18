package com.campusgig.matching.infrastructure.ai;

import com.campusgig.matching.domain.JobSnapshot;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tools the job matching agent can call. The LLM decides when to call them, which is the
 * tool-use agentic pattern: the agent gathers the data it needs instead of being handed it.
 */
@Component
public class JobCatalogTools {

    private static final Logger log = LoggerFactory.getLogger(JobCatalogTools.class);

    private final JobSnapshotRepository jobs;

    public JobCatalogTools(JobSnapshotRepository jobs) {
        this.jobs = jobs;
    }

    @Tool("Lists every open job on CampusGig with its id, title, category, location and description.")
    public String listOpenJobs() {
        log.info("Agent called tool: listOpenJobs");
        return format(jobs.findAll());
    }

    @Tool("Lists open jobs in one category, for example hospitality, tutoring or retail.")
    public String findJobsByCategory(@P("the job category to filter by") String category) {
        log.info("Agent called tool: findJobsByCategory({})", category);
        List<JobSnapshot> matches = jobs.findAll().stream()
                .filter(job -> job.getCategory() != null && job.getCategory().equalsIgnoreCase(category))
                .toList();
        return format(matches);
    }

    private String format(List<JobSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return "No jobs available.";
        }
        return snapshots.stream()
                .map(job -> "jobId=" + job.getJobId()
                        + " | title=" + job.getTitle()
                        + " | category=" + job.getCategory()
                        + " | location=" + job.getLocation()
                        + " | description=" + job.getDescription())
                .collect(Collectors.joining("\n"));
    }
}
