# Job ad critic (reviewer agent)

Reviews the job ad assistant's rewrite before the employer sees it.
Pattern: **reflection** (also called evaluator-optimizer).
Code: `matching-service/.../infrastructure/ai/JobAdCritic.java`, `domain/JobAdGuard.java` and
`service/AiAgentService.java`.

## System prompt

```
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
```

## User prompt

```
Employer's original draft.
Title: {{title}}
Category: {{category}}
Location: {{location}}
Pay: {{pay}}
Description: {{description}}

Rewritten ad to review.
Title: {{improvedTitle}}
Description: {{improvedDescription}}
```

## How the loop runs

1. The writer agent rewrites the draft.
2. `JobAdGuard` (plain Java, no LLM) flags any dollar amount in the rewrite that is not in the draft.
3. The critic agent reviews the rewrite against the draft.
4. If steps 2 and 3 found nothing, the rewrite is returned as it is.
5. Otherwise the problems are sent back to the writer, which revises once. The response to the
   employer lists the problems (`reviewNotes`) and the number of revisions.

The number of review rounds is `campusgig.ai.ad-review-rounds` in `application.yml` (default 1,
0 switches the review off).

## Why it is written this way

- **A separate agent with a different job.** A model asked to "improve" an ad leans towards
  adding selling points. A model asked only to find differences from the original is a better
  check on that than asking the writer to be careful.
- **"Do not flag changes to wording"** keeps the critic from sending every rewrite back, since
  changing the wording is the whole point of the writer.
- **Pay is checked in code as well.** A wrong pay rate in a job ad is the mistake that matters
  most, so it does not depend on a model noticing it.
- **The review is best effort.** If the critic call fails or its verdict cannot be read, the
  employer still gets the first rewrite. A failed review must not cost the employer the result.

A real verdict from Gemini: *paste one here after running `./demo.sh` with the API key set.*
