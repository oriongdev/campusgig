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