# AI-assisted development log

Record every meaningful use of AI tools here as you go. Section 5 of the final report asks for
concrete examples of proposals the team accepted, rejected or modified, with references to
artefacts in this repository. Reconstructing this at the end is much harder than keeping it now.

Format: one entry per session.

---

## Template

**Date:**
**Member:**
**Tool:**
**Task:**
**Prompt (summary or link to `docs/prompts/`):**
**What the AI proposed:**
**Decision (accepted / modified / rejected) and why:**
**Artefacts:** (files, commits)

---

## Entries

### 2026-09-16 — Project scaffolding
**Member:** Orion
**Tool:** Claude
**Task:** Generate the four Spring Boot services, the layered package structure, Kafka events and the REST call between services.
**What the AI proposed:** Four Maven modules with presentation / service / domain / infrastructure packages, five domain events published through Spring Cloud Stream, and a synchronous REST check from application-service to job-service before an application is accepted.
**Decision:** Accepted, then reviewed as a group.
**Artefacts:** `user-service/`, `job-service/`, `application-service/`, `matching-service/`

### 2026-09-16 — Spring Boot version choice
**Member:** Orion
**Tool:** Claude
**Task:** Pick framework versions.
**What the AI proposed:** Spring Boot 4.x was available, but LangChain4j support for it was uncertain.
**Decision:** Modified: pinned Spring Boot 3.5.16 with Spring Cloud 2025.0.3 for compatibility with LangChain4j 1.20.0.
**Artefacts:** `pom.xml`

### 2026-09-16 — Bug found while reviewing generated code
**Member:** Orion
**Tool:** Claude
**Task:** Validate the `EmailAddress` value object.
**What the AI proposed:** Validate the string first, then trim it.
**Decision:** Rejected and fixed: emails with surrounding whitespace were wrongly rejected. Now trimmed and lowercased before validation.
**Artefacts:** `user-service/src/main/java/com/campusgig/user/domain/EmailAddress.java`, `StudentTest`

### 2026-09-18 — Analytics approach
**Member:** Orion
**Tool:** Claude
**Task:** Implement the two real-time analysis requirements.
**What the AI proposed:** Kafka Streams queries with interactive queries over state stores.
**Decision:** Modified: kept in-memory counters as the default and put the Kafka Streams version behind the `streams` profile, because the extra dependency needs the subject coordinator's approval.
**Artefacts:** `matching-service/.../infrastructure/streams/`, `application-streams.yml`

### 2026-09-18 — Agentic AI design
**Member:** Orion
**Tool:** Claude
**Task:** Build the job matching agent and the job ad assistant.
**What the AI proposed:** LangChain4j `AiServices` interfaces with `@Tool` methods so the model looks up jobs itself (tool-use pattern), returning JSON.
**Decision:** Accepted, with defensive parsing added because LLMs often wrap JSON in markdown fences.
**Artefacts:** `matching-service/.../infrastructure/ai/`, `AiAgentService`

### 2026-09-18 — Web UI
**Member:** Orion
**Tool:** Claude
**Task:** Add a browser interface so the system can be demonstrated without Postman.
**What the AI proposed:** A single dependency-free HTML file served by user-service, plus a CORS configuration class in each service.
**Decision:** Accepted. A framework-based frontend was rejected because extra dependencies need the subject coordinator's approval and a frontend is not a marked requirement.
**Artefacts:** `user-service/src/main/resources/static/index.html`, `*/infrastructure/config/WebConfig.java`

### 2026-09-18 — Job editing and trending jobs
**Member:** Orion
**Tool:** Claude
**Task:** Close the two requirements that had no implementation.
**What the AI proposed:** A `JobUpdated` event consumed by matching-service to refresh its local job copy, and a 15-minute sliding window for trending jobs.
**Decision:** Accepted.
**Artefacts:** `job-service/.../JobUpdated.java`, `AnalyticsService.trendingJobs()`, `JobTest`, `AnalyticsServiceTest`

### 2026-09-18 — Test suite
**Member:** Orion
**Tool:** Claude
**Task:** Add tests beyond the single domain test.
**What the AI proposed:** 10 test classes: domain tests, Mockito service tests, a `@DataJpaTest` repository test and a `@WebMvcTest` controller test.
**Decision:** Accepted. All 47 tests pass (`mvn test`, 2026-09-18).
**Artefacts:** `*/src/test/java/...`

### 2026-09-18 — AI-generated integration failed against the real API
**Member:** Orion
**Tool:** Claude
**Task:** Get the job matching agent working against Google Gemini.
**What happened:** The generated `GeminiConfig` was accepted and compiled, but the first live call returned HTTP 400: *"Function call is missing a thought_signature in functionCall parts"*. Gemini 3 models return a thought signature with each function call and require it to be sent back on the following request, which LangChain4j only does when thinking passthrough is enabled.
**Decision:** Modified: added `.returnThinking(true)` and `.sendThinking(true)`. The agent then returned correct recommendations, including availability-aware reasoning the rule-based matcher cannot produce.
**Lesson:** Generated code that compiles and looks right can still fail on provider-specific behaviour that isn't visible in the code. Only running it against the real service exposed this.
**Artefacts:** `matching-service/.../infrastructure/ai/GeminiConfig.java`, `logs/matching-service.log`

### 2026-10-07 — AI-generated Kafka Streams configuration did not start
**Member:** Orion
**Tool:** Claude
**Task:** Run the Kafka Streams profile for the first time. It was generated on 18 September and had only ever been compiled.
**Prompt:** `docs/prompts/development-prompts.md`, session of 7 October
**What happened:** With the `streams` profile on, matching-service failed at start-up: *"Parameter 0 of method jaasInitializer ... required a single bean, but 2 were found: binderConfigurationProperties, kstream-KafkaStreamsBinderConfigurationProperties"*. The generated `application-streams.yml` declared the Kafka and Kafka Streams binders explicitly under `spring.cloud.stream.binders`, which makes Spring create the binder configuration twice.
**Decision:** Rejected the generated configuration. The `binders` section was removed. Spring Cloud Stream picks the Kafka Streams binder by itself for a function that takes a `KStream`, so nothing needs declaring. After the fix both stream applications reached `RUNNING` and the interactive queries returned the right counts.
**Lesson:** The same lesson as the Gemini failure, from the other direction. This time the mistake was configuration that was more explicit than it needed to be. Compiling proved nothing, because the error only appears when the application context starts.
**Artefacts:** `matching-service/src/main/resources/application-streams.yml`

### 2026-10-07 — Persistent stream state disagreed with the in-memory databases
**Member:** Orion
**Tool:** Claude
**Task:** Check the Kafka Streams counts across a restart of the whole system.
**What the AI had proposed (18 September):** Count with the default persistent state stores, which are backed by changelog topics.
**What happened:** After matching-service was restarted its database was empty, but the Kafka Streams counts still included every earlier event. After a restart of the whole system, where ids start again from 1, those old counts would have been shown against new jobs that happen to reuse the same ids.
**Decision:** Modified. The stores are now in-memory with changelogs switched off, and a brand-new stream application starts from the latest offset. The counts therefore start empty with the databases. Persistent stores are the right choice for production and the report says so.
**Lesson:** A default that is correct on its own can be wrong for the system it sits in. The AI's first version was textbook Kafka Streams, but nobody had asked how it interacts with services that forget everything on restart.
**Artefacts:** `StreamProcessors.java`, `application-streams.yml`, `StreamProcessorsTest`

### 2026-10-07 — What happens when the LLM fails
**Member:** Orion
**Tool:** Claude
**Task:** Add error handling to the two AI agents. On 18 September a Gemini error had surfaced to the browser as a bare HTTP 500.
**What the AI proposed:** Treat everything an agent returns as untrusted. For the matching agent: drop recommended jobs that are not really on the board, keep scores between 0 and 1, return at most five, and if the call fails or the reply cannot be parsed, answer with the rule-based `SkillMatcher` and label the response `RULE_BASED_FALLBACK`. For the job ad assistant: return HTTP 502 with a readable message, since there is nothing to fall back to. Add a timeout, two retries and a limit on tool-calling rounds.
**Decision:** Accepted, with one change. A silent fallback was considered and rejected, because an invalid API key would then look like a working agent. The response carries a `source` and a `note` instead, and the UI shows them.
**Checked by:** `AiAgentServiceTest` (19 tests), and by starting matching-service with an unusable key: recommendations came back as a labelled fallback and the ad review returned 502. On 2026-10-11 the team ran `./demo.sh` on Windows with a real `GEMINI_API_KEY`: all 50 checks passed, and the matching agent answered itself (`"source":"AI_AGENT"`).
**Artefacts:** `AiAgentService.java`, `AiRecommendationResult.java`, `GeminiConfig.java`, `AiAgentServiceTest`

### 2026-10-07 — A second agent that reviews the first
**Member:** Orion
**Tool:** Claude
**Task:** Stop the job ad assistant from embellishing an employer's ad.
**What the AI proposed:** A reflection loop. A critic agent with its own prompt compares the rewrite with the employer's draft and lists any changed fact or invented detail, and the writer agent revises its rewrite once if anything is listed. Alongside it, a rule in plain Java (`JobAdGuard`) that flags any dollar amount in the rewrite that was not in the draft.
**Decision:** Accepted. A fully LLM-based check was rejected for the pay rule: a wrong pay rate in a job ad is the one mistake that must not get through, so it is checked by code that cannot be talked out of it. The review is best effort, so if the critic call fails the employer still gets the first rewrite.
**Checked by:** `JobAdGuardTest`, `AiAgentServiceTest`, and `JobAdAgentsPromptTest`, which runs the real LangChain4j prompt templates against a scripted model.
**Artefacts:** `JobAdCritic.java`, `JobAdAssistant.java`, `JobAdGuard.java`, `docs/prompts/job-ad-critic.md`

### 2026-10-07 — Testing an agent without the LLM
**Member:** Orion
**Tool:** Claude
**Task:** Prove the tool-use pattern in an automated test, and extend the test suite.
**What the AI proposed:** A `ScriptedChatModel` that stands in for Gemini and replays fixed replies, so the real LangChain4j `AiServices` loop runs in a unit test: the model asks for the `listOpenJobs` tool, LangChain4j executes it against the job catalogue, and the result goes back to the model. Also controller, repository, publisher and REST-client tests for the other three services, and `demo.sh`, which drives all 14 requirements against the running system.
**Decision:** Accepted. 144 tests pass (`mvn test`, 2026-10-07), and `./demo.sh` passes all 48 checks with the counters and with Kafka Streams.
**Artefacts:** `JobMatchingAgentToolUseTest`, `ScriptedChatModel`, `*/src/test/java/...`, `demo.sh`

### 2026-10-07 — Checking the layering instead of only describing it
**Member:** Orion
**Tool:** Claude
**Task:** Draw the layered architecture diagram for the report.
**What happened:** To draw the diagram the AI listed which layer imports which, across all four services. The domain layer was clean, but one controller (`AnalyticsController`) imported an infrastructure class directly, because it chose between the Kafka Streams results and the in-memory counters itself.
**What the AI proposed:** Move that choice into a new service-layer class, `AnalyticsQueryService`, so the controller only talks to the service layer. Add a `LayeredArchitectureTest` to each service that reads the import statements and fails if the domain layer depends on another layer, or if the presentation and infrastructure layers depend on each other. No new library was needed for it.
**Decision:** Accepted. The test was first run against a deliberately wrong import to confirm that it fails. One dependency is left as it is on purpose and is explained in the report: `EventConsumers` in `infrastructure.messaging` calls the service layer, because a Kafka consumer is an entry point into the service in the same way a REST controller is.
**Lesson:** Writing the report found a design flaw that reading the code had not. The diagram had to be true, so the code was checked against it.
**Checked by:** 162 tests pass (`mvn test`, 2026-10-07).
**Artefacts:** `AnalyticsQueryService.java`, `AnalyticsController.java`, `LayeredArchitectureTest.java` (one per service), `docs/diagrams/layered-architecture.png`

### 2026-10-11 — First run on Windows, and first run against the real Gemini
**Member:** Orion
**Tool:** Claude
**Task:** Run the system on a Windows PC with a real Gemini API key, and record the AI output for the report.
**Prompt:** `docs/prompts/development-prompts.md`, session of 11 October
**What happened:** The README's Windows instructions, written by the AI, said to install Maven with `winget install Apache.Maven`. That package does not exist: Maven is not in winget. The README also assumed Docker Desktop was already installed for Kafka. The key was first set with `set`, which in PowerShell does not create an environment variable.
**Decision:** Rejected the generated install command. The README and `run.bat` now give the manual Maven install in three PowerShell commands, and the AI checked the winget catalogue before suggesting `winget install Docker.DockerDesktop`. With Maven, Docker and `$env:GEMINI_API_KEY` set, `run.bat` started all four services and `./demo.sh` (run through Git Bash) passed all 50 checks.
**What the agents did:** The matching agent recommended the two barista jobs and left out the tutoring jobs. The job ad writer's first rewrite added two duties, a requirement and a perk that were not in the employer's draft. The critic agent listed all three, the writer revised once, and the final ad kept only the draft's facts. This was the first evidence that the reflection loop catches real embellishment, not only the scripted cases in the tests.
**Lesson:** Set-up instructions are code too, and the AI's were never run on the platform they were written for. The first person to follow them on Windows found two gaps in ten minutes.
**Artefacts:** `README.md`, `run.bat`, report section 4.2

<!-- Add new entries above this line -->
