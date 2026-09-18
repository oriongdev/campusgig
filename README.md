# CampusGig

Student job and gig board built for CSCI318 (Spring 2026). Students find casual work, internships and short gigs; employers post jobs and manage applicants.

Four Spring Boot microservices, each owning its own H2 database, talking over REST and Kafka events, plus a web UI served by user-service.

Open **http://localhost:8081** once it's running.

| Service | Port | Owns | Publishes | Consumes |
|---|---|---|---|---|
| user-service | 8081 | Students, employers (and serves the web UI) | `student-profile-updated` | – |
| job-service | 8082 | Job listings | `job-posted`, `job-closed` | – |
| application-service | 8083 | Job applications | `application-submitted`, `application-status-changed` | – (calls job-service over REST) |
| matching-service | 8084 | Job/student snapshots, notifications, analytics | – | all five topics |

## What you need

| | macOS | Windows |
|---|---|---|
| JDK 21 | `brew install --cask temurin@21` | `winget install EclipseAdoptium.Temurin.21.JDK` |
| Maven | `brew install maven` | `winget install Apache.Maven` |
| Kafka | `brew install kafka` (or Docker) | Docker Desktop |

Close and reopen your terminal after installing, then check with `java -version` and `mvn -v`.

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

## Running it from VS Code instead

1. Install the **Extension Pack for Java** and **Spring Boot Extension Pack**.
2. Open this folder (the one with the top-level `pom.xml`) and wait for the Java project to finish loading.
3. Start Kafka: `brew services start kafka` (macOS) or `docker compose up -d` (Windows/Docker).
4. In the Spring Boot Dashboard, start all four services.
5. Open http://localhost:8081.

Kafka must be running before the services start. Restarting one service wipes its data, so restart all four together for a clean run.

## Using the web UI

- **Live analytics tab → Create demo data** sets up a student, an employer and two jobs in one click.
- **Student tab:** register or pick a student, edit your profile, search jobs, apply, and view your applications, notifications and recommendations.
- **Employer tab:** register or pick an employer, post jobs, and shortlist, accept or reject applicants, or close a job.
- **Live analytics tab:** job and application counts, refreshed every 5 seconds.

The dots in the top right show which services are up.

A typical demo: create demo data, search and apply as the student, switch to the employer tab and accept the application, then go back to the student tab to see the notification appear. Those notifications arrive through Kafka, not through a direct call.

## Turning on the AI agents

The two agentic AI features run on Google Gemini through LangChain4j. They're off until you give matching-service an API key.

1. Get a free key at https://aistudio.google.com/apikey
2. Set it before starting the services:

   ```bash
   export GEMINI_API_KEY=your-key      # macOS/Linux
   ```
   ```bat
   set GEMINI_API_KEY=your-key         REM Windows
   ```
3. Start (or restart) matching-service. Its log will say `AI agents enabled using Gemini model ...`

The model is set in `matching-service/src/main/resources/application.yml` (`campusgig.ai.model`), default `gemini-3.1-flash-lite`.

What the agents do:

| Agent | Where to try it | Endpoint |
|---|---|---|
| Job matching agent | Student tab, "Ask the AI agent" | `GET :8084/api/ai/students/{id}/recommendations` |
| Job ad assistant | Employer tab, "Improve with AI" | `POST :8084/api/ai/job-ad/review` |

The matching agent uses the tool-use pattern: it decides when to call `listOpenJobs` or `findJobsByCategory` (see `JobCatalogTools`) instead of being handed the job list, and the tool calls are logged by matching-service. Without a key, the AI buttons are greyed out and the endpoints return 503.

## REST API (no UI needed)

Import `CampusGig.postman_collection.json` into Postman and run the requests in order, or use curl:

```bash
# Register a student and an employer (user-service)
curl -X POST localhost:8081/api/students -H "Content-Type: application/json" \
  -d '{"name":"Alex Nguyen","email":"alex@uowmail.edu.au","skills":["barista","customer service","java"],"availability":"WEEKENDS"}'
curl -X POST localhost:8081/api/employers -H "Content-Type: application/json" \
  -d '{"companyName":"Northbeach Cafe","email":"jobs@northbeachcafe.com.au"}'

# Post and search jobs (job-service)
curl -X POST localhost:8082/api/jobs -H "Content-Type: application/json" \
  -d '{"employerId":1,"title":"Weekend Barista","description":"Make coffee and handle customer service.","category":"hospitality","location":"Wollongong","payAmount":28.50,"payType":"HOURLY"}'
curl "localhost:8082/api/jobs?category=hospitality&minPay=25"

# Apply, then shortlist and accept (application-service)
curl -X POST localhost:8083/api/applications -H "Content-Type: application/json" \
  -d '{"jobId":1,"studentId":1,"coverMessage":"I have two years of barista experience."}'
curl -X PUT localhost:8083/api/applications/1/status -H "Content-Type: application/json" -d '{"status":"SHORTLISTED"}'
curl -X PUT localhost:8083/api/applications/1/status -H "Content-Type: application/json" -d '{"status":"ACCEPTED"}'

# Results built from Kafka events (matching-service)
curl localhost:8084/api/notifications/students/1
curl localhost:8084/api/matching/students/1/recommendations
curl localhost:8084/api/analytics/jobs-per-category
curl localhost:8084/api/analytics/applications-per-job
```

On Windows use Postman rather than curl, because PowerShell mangles the JSON quotes.

## Stream processing (Kafka Streams)

The two real-time analysis requirements have two implementations:

| | Default | `streams` profile |
|---|---|---|
| Jobs per category | in-memory counters | Kafka Streams `KTable`, queried through a state store |
| Applications per job | in-memory counters | Kafka Streams `KTable` |
| Trending jobs | 15-minute sliding window in memory | Kafka Streams 15-minute tumbling window |

`GET :8084/api/analytics/source` says which one served the numbers, and the REST layer falls back to the counters while the stores are still warming up.

The Kafka Streams version needs the `spring-cloud-stream-binder-kafka-streams` dependency, which is an addition beyond the mandatory list, so **ask the subject coordinator before relying on it**. Until then the default profile behaves exactly as before. To turn it on:

```bash
SPRING_PROFILES_ACTIVE=streams mvn -pl matching-service spring-boot:run
```

Topologies live in `matching-service/.../infrastructure/streams/StreamProcessors.java`, with the equivalent SQL-like queries in the class comment for the report.

## Tests

```bash
mvn test
```

| Test | Type | Covers |
|---|---|---|
| `JobApplicationTest` | domain | application status transitions |
| `JobTest` | domain | job creation, search rules, closing, editing |
| `StudentTest` | domain | email value object, skill normalisation |
| `SkillMatcherTest` | domain | rule-based recommendation scoring |
| `JobServiceTest` | service (Mockito) | events published on post, edit and close |
| `ApplicationServiceTest` | service (Mockito) | closed jobs, duplicate applications, status events |
| `AnalyticsServiceTest` | service (Mockito) | counts and the trending window |
| `EventConsumersTest` | messaging | each Kafka consumer delegates correctly |
| `JobApplicationRepositoryTest` | `@DataJpaTest` | derived repository queries |
| `StudentControllerTest` | `@WebMvcTest` | REST status codes and JSON |

## Project structure

Each service uses the same layered package structure:

```
com.campusgig.<service>
├── presentation      REST controllers, request/response DTOs, error handling
├── service           application services (use cases, transactions)
├── domain            entities, value objects, aggregates, domain events
└── infrastructure
    ├── persistence   Spring Data JPA repositories
    ├── messaging     Kafka publishers / consumers (Spring Cloud Stream)
    ├── client        REST clients to other services (application-service only)
    └── config        CORS settings so the browser UI can call every service
```

The web UI is a single file: `user-service/src/main/resources/static/index.html`. It has no build step and no dependencies.

`docs/ai-log.md` records how the team used AI tools during development, which Section 5 of the report requires. Add an entry whenever you use AI on this project.

## Tech stack

Java 21 · Spring Boot 3.5.16 · Spring Web · Spring Data JPA · H2 · Spring Cloud Stream (Kafka binder, Spring Cloud 2025.0.3) · Apache Kafka · LangChain4j 1.20.0

## Current status

Working:
- All four services with REST APIs, layered packages and their own H2 databases
- Web UI for students, employers and live analytics
- Kafka events between services via Spring Cloud Stream
- REST call from application-service to job-service to check a job is open
- Application status rules in the domain, with unit tests (`JobApplicationTest`)
- Two agentic AI components (job matching agent, job ad assistant) on Gemini via LangChain4j
- Job editing with a `JobUpdated` event, and trending jobs
- Unit, service, repository, controller and consumer tests

In progress:
- Stream processing: analytics currently uses simple in-memory counters; to be replaced with Kafka Streams queries (dependency needs coordinator approval)
- AI: agents work but need tuning and error handling; the rule-based `SkillMatcher` is still there as a fallback
- Kafka Streams analytics are written but unproven: they run only under the `streams` profile and still need a live test run once the dependency is approved
- The AI agents need a real run against Gemini with a key
- Report, diagrams and meeting minutes
