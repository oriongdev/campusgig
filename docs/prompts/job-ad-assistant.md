# Job ad assistant (writer agent)

Requirement 14: suggest improvements to an employer's draft job ad.
Pattern: **reflection**, together with the critic in `job-ad-critic.md`.
Code: `matching-service/.../infrastructure/ai/JobAdAssistant.java` and `service/AiAgentService.java`.

## System prompt (used for both calls below)

```
You help small employers write clear job ads for university students on CampusGig.
Keep the employer's facts: never change the pay, location or category, and never
invent duties, hours or requirements that are not in the draft.
Improve clarity, structure and tone, and make the ad appealing to students.
Reply with JSON only, no prose and no markdown fences, in exactly this shape:
{"improvedTitle": "...", "improvedDescription": "...", "tips": ["...", "..."]}
tips lists up to 3 short notes on what was weak in the original draft.
```

## First call: improve

```
Draft job ad.
Title: {{title}}
Category: {{category}}
Location: {{location}}
Pay: {{pay}}
Description: {{description}}
```

## Second call, only if the review finds problems: revise

```
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
```

## Why it is written this way

- **The facts are fenced off.** Pay, location and category are sent so the model can write a
  sensible ad, but the reply has no fields for them, so the model cannot change what is stored.
  It can still contradict them in the text, which is what the review step is for.
- **The reply is a fixed JSON shape** so the UI can put the new title and description straight
  into the form. If a field is missing, the employer's own text is kept.
- **The revise prompt gives the model its own previous answer and the specific complaints.**
  Asking it to "try again" without them tends to produce the same mistake.
- **If the first call fails there is nothing to fall back to**, so the endpoint returns 502 with
  a readable message and the form is left as the employer wrote it.

A real response from Gemini: *paste one here after running `./demo.sh` with the API key set.*
