# CampusGig requirements specification

The requirements the system was built and tested against. The full use cases, with UML activity
diagrams, are in section 2 of the report and in `docs/diagrams/activity/`.

## Stakeholders and objectives

| Objective | Main stakeholder | What they expect |
|---|---|---|
| O1 Find suitable work quickly | Students | Jobs that match their skills and the times they're free |
| O2 Know where every application stands | Students | A clear status for each application, and to be told when it changes |
| O3 Hire with little effort | Employers | Post a job in a minute, get suitable applicants and decide on them easily |
| O4 See demand as it happens | Platform operator | Live figures on which categories and jobs are busy |
| O5 Keep the platform reliable and easy to change | Platform operator | One part can fail or change without taking the rest down |

## Actors

| Actor | Who they are |
|---|---|
| Student | A uni student looking for casual work, an internship or a short gig |
| Employer | A business that posts jobs and hires students |
| Platform operator | The person running CampusGig, who watches the live figures |
| Gemini (external system) | The LLM the two AI agents run on |

## Functional requirements

| # | Requirement | Actor | Service | Kind | Objectives | Checked by |
|---|---|---|---|---|---|---|
| R1 | A student can register a profile with their skills and availability | Student | user-service | Core | O1 | `demo.sh` step 1, `UserServiceTest`, `StudentControllerTest` |
| R2 | An employer can register an account | Employer | user-service | Core | O3 | `demo.sh` step 2, `UserServiceTest` |
| R3 | A student can update their skills and availability | Student | user-service | Core | O1 | `demo.sh` step 3, `UserServiceTest` |
| R4 | An employer can post a job with a category, location and pay | Employer | job-service | Core | O3 | `demo.sh` step 4, `JobServiceTest`, `JobControllerTest` |
| R5 | An employer can edit or close a job | Employer | job-service | Core | O2, O3 | `demo.sh` steps 5 and 5 (continued), `JobTest` |
| R6 | A student can search open jobs by category, location and minimum pay | Student | job-service | Core | O1 | `demo.sh` step 6, `JobTest` |
| R7 | A student can apply for an open job with a cover message | Student | application-service | Core | O1, O3 | `demo.sh` step 7, `ApplicationServiceTest` |
| R8 | A student can withdraw an application | Student | application-service | Core | O2, O3 | `demo.sh` step 8, `ApplicationServiceTest` |
| R9 | An employer can shortlist, accept or reject an applicant | Employer | application-service | Core | O2, O3 | `demo.sh` step 9, `JobApplicationTest` |
| R10 | A student is notified when the status of their application changes | Student | matching-service | Core | O2 | `demo.sh` step 10, `NotificationServiceTest` |
| R11 | The platform shows a live count of jobs posted per category | Platform operator | matching-service | Real-time analysis | O4 | `demo.sh` step 11, `AnalyticsServiceTest`, `StreamProcessorsTest` |
| R12 | The platform shows live applications per job and which jobs are trending | Platform operator | matching-service | Real-time analysis | O4 | `demo.sh` step 12, `AnalyticsServiceTest`, `StreamProcessorsTest` |
| R13 | An AI agent recommends suitable open jobs to a student | Student | matching-service | Agentic AI | O1 | `demo.sh` step 13, `AiAgentServiceTest`, `JobMatchingAgentToolUseTest` |
| R14 | An AI assistant improves an employer's draft job ad without changing the facts | Employer | matching-service | Agentic AI | O3 | `demo.sh` step 14, `AiAgentServiceTest`, `JobAdGuardTest` |

## Business rules

- An email address is trimmed, lower-cased and must look like `name@domain.tld`. Each email can register once as a student and once as an employer.
- A job needs a title, category, location and pay. Pay must be above zero and is either HOURLY or FIXED.
- A closed job can't be edited, closed again, or applied for, and isn't shown in search.
- A student can apply for a job only once.
- Application status changes: SUBMITTED can go to SHORTLISTED, ACCEPTED, REJECTED or WITHDRAWN; SHORTLISTED can go to ACCEPTED, REJECTED or WITHDRAWN; ACCEPTED, REJECTED and WITHDRAWN are final.
- A job is trending when it has two or more applications in the last 15 minutes.
- The matching agent may only return open jobs, at most five, with scores between 0 and 1.
- The job ad assistant must not add or change a dollar amount. Any amount in the rewrite that isn't in the draft is flagged by `JobAdGuard`.

## Architectural requirements (from the subject specification)

| Requirement | How CampusGig meets it |
|---|---|
| At least four bounded contexts, each a microservice | user, job, application and matching services |
| Event-driven communication | Six domain events on Kafka through Spring Cloud Stream |
| Stream processing for real-time queries | R11 and R12, with Kafka Streams (`--streams`) or in-memory counters |
| LLM-based agentic components | R13 (tool use) and R14 (reflection), LangChain4j on Gemini |
| Spring Boot with Spring Web, Spring Data JPA, H2, Spring Cloud Stream, LangChain4j | All used. The Kafka Streams binder is the one extra dependency, behind a profile until approved |
| JDK 21 | Compiled for Java 21 |
| Apache Kafka | Kafka 3.9 (Docker) or Homebrew Kafka |
