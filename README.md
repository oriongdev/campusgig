# CampusGig

Student job and gig board built for CSCI318 (Spring 2026). Students find casual work, internships and short gigs; employers post jobs and manage applicants.

Four Spring Boot microservices, each owning its own H2 database, talking over REST and Kafka events, plus a web UI served by user-service. Two real-time analysis queries run on Kafka Streams, and two agentic AI features run on Google Gemini through LangChain4j.

Open **http://localhost:8081** once it's running.

| Service | Port | Owns | Publishes | Consumes |
|---|---|---|---|---|
| user-service | 8081 | Students, employers (and serves the web UI) | `student-profile-updated` | – |
| job-service | 8082 | Job listings | `job-posted`, `job-updated`, `job-closed` | – |
| application-service | 8083 | Job applications | `application-submitted`, `application-status-changed` | – (calls job-service over REST) |
| matching-service | 8084 | Job and student read models, notifications, analytics, AI agents | – | all six topics |

## What you need

| | macOS | Windows |
|---|---|---|
| JDK 21 | `brew install --cask temurin@21` | `winget install EclipseAdoptium.Temurin.21.JDK` |
| Maven | `brew install maven` | See *Maven on Windows* below |
| Kafka | `brew install kafka` (or Docker) | Docker Desktop |

**Maven on Windows.** Maven is not in winget. In PowerShell, this downloads it into your user folder and adds it to your `Path` (no admin rights needed):

```powershell
Invoke-WebRequest https://archive.apache.org/dist/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.zip -OutFile "$env:TEMP\maven.zip"
Expand-Archive "$env:TEMP\maven.zip" -DestinationPath "$env:USERPROFILE\tools" -Force
[Environment]::SetEnvironmentVariable("Path", [Environment]::GetEnvironmentVariable("Path", "User") + ";$env:USERPROFILE\tools\apache-maven-3.9.11\bin", "User")
```

Close and reopen your terminal after installing, then check with `java -version` and `mvn -v`. The code is compiled for Java 21. A newer JDK also builds and runs it, but the subject asks for 21.

## Running it (one command)

**macOS / Linux**

```bash
./run.sh
```

**Windows**

```bat
run.bat
```

The script starts Kafka, starts all four services, waits for them, and opens the UI at http://localhost:8081. The first run downloads the Spring and Kafka libraries, so give it a few minutes.

Stop everything with `./stop.sh` on macOS, or on Windows close the four service windows and run `docker compose down`.

All data is held in memory, so every start is a clean slate. Restarting one service on its own wipes only that service's data and leaves the others out of step, so always restart all four together.

## Checking that everything works

With the system running:

```bash
./demo.sh
```

This walks through all 14 functional requirements with `curl`, prints each request and response, and checks every result (48 checks, or 50 with the AI agents switched on). It finishes with `All checks passed`. Its output is also the sample input and output for each user story. It registers new users each time, so you can run it again without restarting.

## Running it from VS Code instead

1. Install the **Extension Pack for Java** and **Spring Boot Extension Pack**.
2. Open this folder (the one with the top-level `pom.xml`) and wait for the Java project to finish loading.
3. Start Kafka: `brew services start kafka` (macOS) or `docker compose up -d` (Windows/Docker).
4. In the Spring Boot Dashboard, start all four services.
5. Open http://localhost:8081.

Kafka must be running before the services start.

## Using the web UI

- **Live analytics tab → Create demo data** sets up a student, an employer and two jobs in one click.
- **Student tab:** register or pick a student, edit your profile, search jobs, apply, withdraw, and view your applications, notifications and recommendations.
- **Employer tab:** register or pick an employer, post and edit jobs, improve a draft ad with AI, shortlist, accept or reject applicants, or close a job.
- **Live analytics tab:** jobs per category, applications per job and trending jobs, refreshed every 5 seconds.

The dots in the top right show which services are up.

A typical demo: create demo data, search and apply as the student, switch to the employer tab and accept the application, then go back to the student tab to see the notification appear. Those notifications arrive through Kafka, not through a direct call.

## Turning on the AI agents

The two agentic AI features run on Google Gemini through LangChain4j. They're off until you give matching-service an API key.

1. Get a free key at https://aistudio.google.com/apikey
2. Set it **in the same terminal, before starting the services**:

   ```bash
   export GEMINI_API_KEY=your-key      # macOS/Linux
   ```
   ```bat
   set GEMINI_API_KEY=your-key         REM Windows
   ```
3. Start (or restart) the services. The matching-service log will say `AI agents enabled using Gemini model ...`

`export` only lasts for that terminal window. If the two AI buttons are greyed out, the key wasn't set in the terminal that started matching-service: stop, export it again and start again. To set it once for every new terminal on macOS, run `echo 'export GEMINI_API_KEY=your-key' >> ~/.zshrc`. Never put the key in a file that is committed to the repository.

The model is set in `matching-service/src/main/resources/application.yml` (`campusgig.ai.model`), default `gemini-3.1-flash-lite`.

| Agent | Pattern | Where to try it | Endpoint |
|---|---|---|---|
| Job matching agent | Tool use | Student tab, "Ask the AI agent" | `GET :8084/api/ai/students/{id}/recommendations` |
| Job ad assistant | Reflection (writer and critic) | Employer tab, "Improve with AI" | `POST :8084/api/ai/job-ad/review` |

**Job matching agent.** The agent is not handed the list of jobs. It decides when to call the `listOpenJobs` or `findJobsByCategory` tools (see `JobCatalogTools`) and then ranks what it finds against the student's skills and availability. Each tool call is logged by matching-service (`grep "Agent called tool" logs/matching-service.log`). Its answer is checked before it is returned: jobs that are not really on the board are dropped and scores are kept between 0 and 1. If the LLM is slow, over quota or replies with something unusable, the rule-based `SkillMatcher` answers instead and the response says so (`"source": "RULE_BASED_FALLBACK"`).

**Job ad assistant.** A writer agent rewrites the employer's draft. A second critic agent then compares the rewrite with the draft and flags anything that changed a fact or invented a detail, and a rule in plain code (`JobAdGuard`) flags any dollar amount that was not in the draft. If either finds a problem, the writer revises its rewrite once. The response lists what the reviewer found (`reviewNotes`) and how many revisions were made.

Without a key, the AI buttons are greyed out and the two endpoints return 503. Everything else works.

## REST API (no UI needed)

Import `CampusGig.postman_collection.json` into Postman and run the requests in order, or use curl:

```bash
# Register a student and an employer (user-service)
curl -X POST localhost:8081/api/students -H "Content-Type: application/json" \
  -d '{"name":"Alex Nguyen","email":"alex@uowmail.edu.au","skills":["barista","customer service","java"],"availability":"WEEKENDS"}'
curl -X POST localhost:8081/api/employers -H "Content-Type: application/json" \
  -d '{"companyName":"Northbeach Cafe","email":"jobs@northbeachcafe.com.au"}'

# Post, edit and search jobs (job-service)
curl -X POST localhost:8082/api/jobs -H "Content-Type: application/json" \
  -d '{"employerId":1,"title":"Weekend Barista","description":"Make coffee and handle customer service.","category":"hospitality","location":"Wollongong","payAmount":28.50,"payType":"HOURLY"}'
curl -X PUT localhost:8082/api/jobs/1 -H "Content-Type: application/json" \
  -d '{"employerId":1,"title":"Weekend Barista","description":"Make coffee and close up on Sundays.","category":"hospitality","location":"North Wollongong","payAmount":30,"payType":"HOURLY"}'
curl "localhost:8082/api/jobs?category=hospitality&minPay=25"

# Apply, then shortlist and accept, or withdraw (application-service)
curl -X POST localhost:8083/api/applications -H "Content-Type: application/json" \
  -d '{"jobId":1,"studentId":1,"coverMessage":"I have two years of barista experience."}'
curl -X PUT localhost:8083/api/applications/1/status -H "Content-Type: application/json" -d '{"status":"SHORTLISTED"}'
curl -X PUT localhost:8083/api/applications/1/status -H "Content-Type: application/json" -d '{"status":"ACCEPTED"}'
curl -X POST localhost:8083/api/applications/2/withdraw

# Results built from Kafka events (matching-service)
curl localhost:8084/api/notifications/students/1
curl localhost:8084/api/matching/students/1/recommendations
curl localhost:8084/api/analytics/jobs-per-category
curl localhost:8084/api/analytics/applications-per-job
curl localhost:8084/api/analytics/trending-jobs
curl localhost:8084/api/analytics/source

# Close a job (job-service)
curl -X POST localhost:8082/api/jobs/1/close

# The two AI agents (matching-service, needs GEMINI_API_KEY)
curl localhost:8084/api/ai/status
curl localhost:8084/api/ai/students/1/recommendations
curl -X POST localhost:8084/api/ai/job-ad/review -H "Content-Type: application/json" \
  -d '{"title":"barista wanted","category":"hospitality","location":"Wollongong","pay":"28.50 HOURLY","description":"need someone for weekends. must be good with people"}'
```

Errors come back as RFC 7807 problem JSON with a `detail` message: 400 for a broken rule in the request, 404 for something that doesn't exist, 409 for a conflict such as applying twice or editing a closed job, 502 if the LLM fails, 503 if the AI is not configured or job-service is down.

On Windows use Postman rather than curl, because PowerShell mangles the JSON quotes.

## Stream processing (Kafka Streams)

The two real-time analysis requirements have two implementations, and matching-service can run either:

| | `./run.sh` (default) | `./run.sh --streams` |
|---|---|---|
| Jobs per category | counters updated by a Kafka consumer | Kafka Streams `KTable`, read through an interactive query |
| Applications per job | counters updated by a Kafka consumer | Kafka Streams `KTable`, read through an interactive query |
| Trending jobs | 15-minute sliding window in memory | Kafka Streams 15-minute tumbling window |

`GET :8084/api/analytics/source` (and the Live analytics tab) says which one is answering. For the few seconds Kafka Streams takes to start, the counters answer instead.

The Kafka Streams version needs the `spring-cloud-stream-binder-kafka-streams` dependency, which is beyond the subject's mandatory list. **Ask the subject coordinator before submitting with it switched on.** Both versions are tested and give the same numbers, so the choice is only about which one you present:

- **If it's approved,** run and demo with `./run.sh --streams` (Windows: `run.bat streams`).
- **If it's not,** stay on the default and present the Kafka Streams topology as the design you would use with that dependency.

To start only matching-service with it:

```bash
SPRING_PROFILES_ACTIVE=streams mvn -pl matching-service spring-boot:run
```

The topologies are in `matching-service/.../infrastructure/streams/StreamProcessors.java`, with the equivalent SQL-like queries in the class comment. The state stores are in-memory, like the H2 databases, so the counts always describe the current run.

## Tests

```bash
mvn test
```

162 tests in 32 classes. They need no Kafka, no running services and no API key.

| Service | Test | Type | Covers |
|---|---|---|---|
| user | `StudentTest` | domain | email value object, skill normalisation |
| user | `UserServiceTest` | service (Mockito) | registration rules, events published on register and update |
| user | `UserRepositoriesTest` | `@DataJpaTest` | lookups by email, skills stored and read back |
| user | `StudentControllerTest` | `@WebMvcTest` | REST status codes and JSON |
| job | `JobTest` | domain | job creation, search rules, closing, editing |
| job | `JobServiceTest` | service (Mockito) | events published on post, edit and close |
| job | `JobRepositoryTest` | `@DataJpaTest` | open jobs, jobs by employer, the `Pay` value object |
| job | `JobEventPublisherTest` | messaging | each event goes to its own binding |
| job | `JobControllerTest` | `@WebMvcTest` | status codes for post, edit, search, close and errors |
| application | `JobApplicationTest` | domain | application status transitions |
| application | `ApplicationServiceTest` | service (Mockito) | closed jobs, duplicate applications, status events, withdrawing |
| application | `JobApplicationRepositoryTest` | `@DataJpaTest` | derived repository queries |
| application | `JobServiceClientTest` | `@RestClientTest` | the REST call to job-service against a fake job-service |
| application | `ApplicationControllerTest` | `@WebMvcTest` | status codes, including 503 when job-service is down |
| matching | `SkillMatcherTest` | domain | rule-based recommendation scoring |
| matching | `JobAdGuardTest` | domain | pay amounts the AI must not change |
| matching | `MatchingServiceTest` | service (Mockito) | read models built from events, ranking |
| matching | `NotificationServiceTest` | service (Mockito) | notification text |
| matching | `AnalyticsServiceTest` | service | counts and the trending window |
| matching | `AnalyticsQueryServiceTest` | service (Mockito) | whether Kafka Streams or the counters answer, and when |
| matching | `AiAgentServiceTest` | service (Mockito) | checking agent answers, fallback, the review loop |
| matching | `EventConsumersTest` | messaging | each Kafka consumer delegates correctly |
| matching | `StreamProcessorsTest` | Kafka Streams | topology shape, state stores, event serdes |
| matching | `JobMatchingAgentToolUseTest` | LangChain4j | the tool-use loop, with a scripted model in place of Gemini |
| matching | `JobAdAgentsPromptTest` | LangChain4j | writer and critic prompt templates |
| matching | `JobCatalogToolsTest` | tools | what the agent's tools return |
| matching | `AnalyticsControllerTest` | `@WebMvcTest` | JSON of the analytics endpoints |
| matching | `AiControllerTest` | `@WebMvcTest` | AI endpoints, 502 and 503 handling |
| all four | `LayeredArchitectureTest` | architecture | the layering rules below, checked from the import statements |

The Kafka path between the services is checked end to end by `./demo.sh` against the running system.

## Project structure

Each service uses the same layered package structure:

```
com.campusgig.<service>
├── presentation      REST controllers, request/response DTOs, error handling
├── service           application services (use cases, transactions)
├── domain            entities, value objects, aggregates, domain services, domain events
└── infrastructure
    ├── persistence   Spring Data JPA repositories
    ├── messaging     Kafka publishers / consumers (Spring Cloud Stream)
    ├── client        REST clients to other services (application-service only)
    ├── streams       Kafka Streams topologies and interactive queries (matching-service only)
    ├── ai            LangChain4j agents and their tools (matching-service only)
    └── config        CORS settings so the browser UI can call every service
```

The layers follow three rules, which `LayeredArchitectureTest` checks in every service: the domain layer depends on no other layer, the service layer does not depend on the presentation layer, and the presentation and infrastructure layers do not depend on each other.

The web UI is a single file: `user-service/src/main/resources/static/index.html`. It has no build step and no dependencies.

| Folder | What's in it |
|---|---|
| `docs/ai-log.md` | How the team used AI tools during development, with the decisions taken. Section 5 of the report draws on it. Add an entry whenever you use AI on this project. |
| `docs/prompts/` | The prompts behind the AI-assisted work, and the prompts the two agents run on. |
| `docs/specification/` | The requirements specification (objectives, the 14 requirements, business rules) and the REST API specification. |
| `docs/diagrams/` | The UML, architecture and database diagrams used in the report, with their sources. |
| `docs/meeting-minutes-template.md` | Template for the weekly meeting minutes. |

## Tech stack

Java 21 · Spring Boot 3.5.16 · Spring Web · Spring Data JPA · H2 · Spring Cloud Stream (Kafka binder and Kafka Streams binder, Spring Cloud 2025.0.3) · Apache Kafka · LangChain4j 1.20.0 · Google Gemini

## Status

All 14 functional requirements are implemented and covered by `./demo.sh`.

- Four services with REST APIs, layered packages and their own H2 databases
- Web UI for students, employers and live analytics
- Six Kafka event types between services via Spring Cloud Stream
- REST call from application-service to job-service to check a job is open
- Two real-time analysis queries on Kafka Streams (switchable, see above)
- Two agentic AI components on Gemini via LangChain4j, with checks on what they return
- 162 unit, service, repository, controller, messaging, agent and architecture tests

Known limits of the prototype:

- All data is in memory and is lost on restart.
- application-service checks that a job exists and is open, but does not check that the student exists. job-service does not check that the employer exists.
- Events on different topics can arrive out of order. If a status change is processed before the job's own event, the notification names the job by number instead of by title.
- There is no login. The UI lets you pick which student or employer you are.
