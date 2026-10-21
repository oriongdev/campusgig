package com.campusgig.matching.infrastructure.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agentic AI component: helps an employer improve a draft job ad before they post it.
 *
 * <p>This is the "writer" half of a reflection loop. {@link JobAdCritic} reviews what it writes,
 * and {@link #revise} lets it fix any problems the review finds. LangChain4j builds the
 * implementation from the prompts below.
 */
@SystemMessage("""
        You help small employers write clear job ads for university students on CampusGig.
        Keep the employer's facts: never change the pay, location or category, and never
        invent duties, hours or requirements that are not in the draft.
        Improve clarity, structure and tone, and make the ad appealing to students.
        Reply with JSON only, no prose and no markdown fences, in exactly this shape:
        {"improvedTitle": "...", "improvedDescription": "...", "tips": ["...", "..."]}
        tips lists up to 3 short notes on what was weak in the original draft.
        """)
public interface JobAdAssistant {

    @UserMessage("""
            Draft job ad.
            Title: {{title}}
            Category: {{category}}
            Location: {{location}}
            Pay: {{pay}}
            Description: {{description}}
            """)
    String improve(@V("title") String title,
                   @V("category") String category,
                   @V("location") String location,
                   @V("pay") String pay,
                   @V("description") String description);

    @UserMessage("""
            Employer's original draft.
            Title: {{title}}
            Category: {{category}}
            Location: {{location}}
            Pay: {{pay}}
            Description: {{description}}

            Your previous rewrite.
            Title: {{improvedTitle}}
            Description: {{improvedDescription}}

            A reviewer found these problems with your rewrite:
            {{issues}}

            Rewrite the ad again and fix every problem. Keep the tips about the original draft.
            """)
    String revise(@V("title") String title,
                  @V("category") String category,
                  @V("location") String location,
                  @V("pay") String pay,
                  @V("description") String description,
                  @V("improvedTitle") String improvedTitle,
                  @V("improvedDescription") String improvedDescription,
                  @V("issues") String issues);
}
