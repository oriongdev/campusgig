package com.campusgig.matching.infrastructure.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agentic AI component: reviews the job ad assistant's rewrite before the employer sees it.
 *
 * <p>This is the "critic" half of a reflection loop (also called evaluator-optimizer). A second
 * LLM call with a different role checks the first one's work, because a model asked only to
 * improve an ad tends to embellish it.
 */
public interface JobAdCritic {

    @SystemMessage("""
            You are a careful reviewer on CampusGig. You check a rewritten job ad against the
            employer's original draft before it is shown to the employer.
            Flag only real problems:
            - a fact was changed (pay, location, category, hours or dates);
            - a duty, requirement, perk or number was added that is not in the original draft;
            - the ad is misleading, discriminatory or unprofessional.
            Do not flag changes to wording, structure or tone.
            Reply with JSON only, no prose and no markdown fences, in exactly this shape:
            {"approved": true, "issues": []}
            If there are problems, set approved to false and list each one as one short sentence
            in issues.
            """)
    @UserMessage("""
            Employer's original draft.
            Title: {{title}}
            Category: {{category}}
            Location: {{location}}
            Pay: {{pay}}
            Description: {{description}}

            Rewritten ad to review.
            Title: {{improvedTitle}}
            Description: {{improvedDescription}}
            """)
    String review(@V("title") String title,
                  @V("category") String category,
                  @V("location") String location,
                  @V("pay") String pay,
                  @V("description") String description,
                  @V("improvedTitle") String improvedTitle,
                  @V("improvedDescription") String improvedDescription);
}
