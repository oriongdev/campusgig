package com.campusgig.matching.infrastructure.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agentic AI component: recommends jobs for a student.
 * LangChain4j builds the implementation and handles the tool calls declared in JobCatalogTools.
 */
public interface JobMatchingAgent {

    @SystemMessage("""
            You are the job matching agent for CampusGig, a casual job board for university students.
            Use the available tools to look up the jobs currently on the board. Never invent a job.
            Judge each job on the student's skills and availability, and prefer jobs the student
            could realistically do.
            Reply with JSON only, no prose and no markdown fences, in exactly this shape:
            [{"jobId": 1, "score": 0.8, "reason": "one short sentence"}]
            score is between 0 and 1. Return at most 5 jobs, best first.
            If no job is a reasonable fit, return [].
            """)
    @UserMessage("Student profile: {{profile}}")
    String recommendJobs(@V("profile") String profile);
}
