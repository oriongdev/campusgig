# Development prompts

The prompts given to the AI assistant (Claude) while building CampusGig, in order, with what each
one led to. They are quoted as typed wherever the wording was kept. `docs/ai-log.md` records what was accepted, changed or
rejected as a result.

Member for every session below: Orion. Add the other members' sessions as they happen.

## Session 1: 16 to 18 September 2026

| # | Prompt | What it produced | Log entry |
|---|---|---|---|
| 1 | "this is our powerpoint" (the Week 4 project plan deck was attached) | A review of the project plan | – |
| 2 | "ok so what should we do" | A work plan and the 14 draft functional requirements | – |
| 3 | Follow-up requests to build the services (not kept word for word) | The four Spring Boot services, layered packages, Kafka events, the REST call between services and the Postman collection | Project scaffolding; Spring Boot version choice; Bug found while reviewing generated code |
| 4 | "can you remake it with a user interface" and then "remake it i want a complete user interface and easy to setup / use on any machine" | The single-file web UI, CORS configuration, `run.sh`, `run.bat` and `stop.sh` | Web UI |
| 5 | The choice of Gemini as the LLM (not kept word for word) | The job matching agent with its tools, and the job ad assistant | Agentic AI design |
| 6 | "is there anything else we need to add or is that good to submit for the final" | A gap list against the specification: stream processing, two unfinished requirements, tests, the report | – |
| 7 | "can you add EVERYTHING we need in the code" | Job editing with `JobUpdated`, trending jobs, the Kafka Streams topologies behind a profile, ten test classes | Job editing and trending jobs; Analytics approach; Test suite |
| 8 | "i clicked ask the ai agent and it just returned '500'", followed by the error from `logs/matching-service.log` | The diagnosis of Gemini's thought-signature requirement and the two-line fix in `GeminiConfig` | AI-generated integration failed against the real API |
| 9 | "give me the latest ai logs" | `docs/ai-log.md` brought up to nine entries | – |

## Session 2: 7 October 2026

| # | Prompt | What it produced | Log entry |
|---|---|---|---|
| 10 | "here is the campusgig project how do i run it again" | Run instructions; the cause of the greyed-out AI button (the API key is per terminal) | – |
| 11 | "is this completed for week 8 or 12 do you remember" | A status check against the Week 12 deliverables | – |
| 12 | "finish it 100% for week 12" (sent a second time while the work was under way) | See below | All six entries dated 2026-10-07 |

Before starting on prompt 12 the assistant asked two questions. The answers were:

- *Has your coordinator approved the Kafka Streams dependency?* "Haven't asked / no reply". So the
  Kafka Streams analytics were made to work and tested, but left switchable rather than default.
- *Which Week 12 pieces should I make besides the code?* "do everything now".

What prompt 12 produced in the code:

- The first real run of the whole test suite, of all four services against a Kafka broker, and of
  the Kafka Streams profile. The profile did not start and was fixed.
- In-memory state stores for the stream processing.
- Error handling, output checks and a rule-based fallback for the matching agent.
- A critic agent and a rule-based guard for the job ad assistant.
- A fix to the layering found while drawing the architecture diagrams, and a test that checks it.
- 115 more tests (47 to 162) and `demo.sh`.

It also produced the first draft of the report, the diagrams in `docs/diagrams/` and the final
presentation. Those are drafts for the team to check, correct and complete.

## Session 3: 11 October 2026

| # | Prompt | What it produced | Log entry |
|---|---|---|---|
| 13 | "where do i put the gemini api key" | How to set the key per terminal, and why it never goes in a file | – |
| 14 | The output of a Maven build, then "i exported the key and this happened" | The diagnosis: the PC was Windows, so `export` does not apply and `mvn install` only builds | First run on Windows |
| 15 | Screenshots of `run.bat` failing: Maven not found, then Docker not found | The corrected Maven install, the fix to the README and `run.bat`, and the Docker Desktop install | First run on Windows |
| 16 | "website is running how i test everything works" | A click-through test of all 14 requirements, and how to run `demo.sh` through Git Bash | – |
| 17 | The full output of `./demo.sh` | Steps 13 and 14 copied into report section 4.2; this log updated | First run on Windows |

## Session 4: 19 October 2026

| # | Prompt | What it produced | Log entry |
|---|---|---|---|
| 18 | "so this is 100% ready to submit" | A list of what was still left for the group to do | – |
| 19 | The official project specification, pasted in full | A check of the report against the specification's required structure, the database design diagram, and `docs/specification/` | – |
| 20 | A request to rewrite the report in a plainer, student voice (not kept word for word) | The report rewritten in plainer language with Australian spelling, and reorganised to follow the specification's section order | – |

## What the assistant could not do

Recorded here because the report's lessons-learned section asks for it.

- It could not call Google Gemini from its own environment, so both agents were tested with a
  scripted model and with the LLM unreachable, not against the real service. The team ran
  `./demo.sh` with a real key on 11 October, and all 50 checks passed.
- It could not test `run.bat` on Windows. When the team did, the Maven install command it had
  written turned out not to exist.
- It could not write the meeting minutes, the contribution ratings or anything else that records
  what the team did. Those have to come from the team.
