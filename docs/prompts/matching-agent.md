# Job matching agent

Requirement 13: recommend jobs to a student based on their profile.
Pattern: **tool use**. Code: `matching-service/.../infrastructure/ai/JobMatchingAgent.java`,
`JobCatalogTools.java`, `GeminiConfig.java`, and `service/AiAgentService.java`.

## System prompt

```
You are the job matching agent for CampusGig, a casual job board for university students.
Use the available tools to look up the jobs currently on the board. Never invent a job.
Judge each job on the student's skills and availability, and prefer jobs the student
could realistically do.
Reply with JSON only, no prose and no markdown fences, in exactly this shape:
[{"jobId": 1, "score": 0.8, "reason": "one short sentence"}]
score is between 0 and 1. Return at most 5 jobs, best first.
If no job is a reasonable fit, return [].
```

## User prompt

```
Student profile: {{profile}}
```

`{{profile}}` is built by `AiAgentService` from the student's read model, for example
`name: Alex Nguyen; skills: barista, customer service; availability: WEEKENDS`.

## Tools the agent may call

| Tool | Description given to the model |
|---|---|
| `listOpenJobs()` | Lists every open job on CampusGig with its id, title, category, location and description. |
| `findJobsByCategory(category)` | Lists open jobs in one category, for example hospitality, tutoring or retail. |

Both return one line per job, for example
`jobId=1 | title=Weekend Barista | category=hospitality | location=Wollongong | description=Make coffee`,
or the sentence `No jobs available.`

## Why it is written this way

- **The jobs are not in the prompt.** The agent has to fetch them with a tool, so it decides what
  it needs (everything, or one category). This is what makes it an agent rather than a single
  prompt, and the tool calls are visible in the log.
- **"Never invent a job"** is also enforced in code. `AiAgentService` drops any recommended job id
  that is not an open job, and takes the title from our own data rather than from the model.
- **"JSON only"** is not relied on. The service extracts the JSON from whatever comes back, keeps
  scores between 0 and 1, removes duplicates, and keeps the best five.
- **An empty catalogue is reported in words** (`No jobs available.`) so the model has something
  definite to read instead of an empty tool result.
- **Tool-calling rounds are capped at 5** and each call times out after 60 seconds.
- **If anything fails**, the rule-based `SkillMatcher` answers and the response is labelled
  `RULE_BASED_FALLBACK`.

## Example exchange

From `JobMatchingAgentToolUseTest`, which runs the real LangChain4j loop with a scripted model:

1. To the model: the system prompt, `Student profile: name: Alex; skills: barista; availability: WEEKENDS`,
   and the two tool descriptions.
2. From the model: a request to call `listOpenJobs`.
3. To the model: `jobId=1 | title=Weekend Barista | category=hospitality | location=Wollongong | description=Make coffee`
4. From the model: `[{"jobId": 1, "score": 0.9, "reason": "Barista skills match"}]`

A real response from Gemini: *paste one here after running `./demo.sh` with the API key set.*
