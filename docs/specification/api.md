# CampusGig REST API specification

All bodies are JSON. Errors are RFC 7807 problem JSON with a `detail` message: 400 for a broken
rule, 404 for something that doesn't exist, 409 for a conflict, 502 if the LLM fails, and 503 if
the AI isn't configured or job-service is down. Sample requests and responses for every endpoint
used by the user stories are in report section 4.2, and `demo.sh` and
`CampusGig.postman_collection.json` call them all.

## user-service (http://localhost:8081)

| Method and path | Body | Success | Errors | Event published |
|---|---|---|---|---|
| `POST /api/students` | `{"name", "email", "skills": [..], "availability": "WEEKDAYS\|WEEKENDS\|EVENINGS\|FLEXIBLE"}` | 201 student | 400, 409 | StudentProfileUpdated |
| `GET /api/students` | – | 200 list of students | – | – |
| `GET /api/students/{id}` | – | 200 student | 404 | – |
| `PUT /api/students/{id}/profile` | `{"skills": [..], "availability"}` | 200 student | 400, 404 | StudentProfileUpdated |
| `POST /api/employers` | `{"companyName", "email"}` | 201 employer | 400, 409 | – |
| `GET /api/employers` | – | 200 list of employers | – | – |
| `GET /api/employers/{id}` | – | 200 employer | 404 | – |

## job-service (http://localhost:8082)

| Method and path | Body | Success | Errors | Event published |
|---|---|---|---|---|
| `POST /api/jobs` | `{"employerId", "title", "description", "category", "location", "payAmount", "payType": "HOURLY\|FIXED"}` | 201 job | 400 | JobPosted |
| `PUT /api/jobs/{id}` | same as POST | 200 job | 400, 404, 409 (closed) | JobUpdated |
| `POST /api/jobs/{id}/close` | – | 200 job | 404, 409 (already closed) | JobClosed |
| `GET /api/jobs/{id}` | – | 200 job | 404 | – |
| `GET /api/jobs?category=&location=&minPay=` | – | 200 list of open jobs matching every filter given | 400 | – |
| `GET /api/jobs/employer/{employerId}` | – | 200 all of an employer's jobs | – | – |

## application-service (http://localhost:8083)

| Method and path | Body | Success | Errors | Event published |
|---|---|---|---|---|
| `POST /api/applications` | `{"jobId", "studentId", "coverMessage"}` | 201 application | 400, 404 (job), 409 (closed or duplicate), 503 (job-service down) | ApplicationSubmitted |
| `GET /api/applications/{id}` | – | 200 application | 404 | – |
| `GET /api/applications?jobId=` or `?studentId=` | – | 200 list of applications | – | – |
| `PUT /api/applications/{id}/status` | `{"status": "SHORTLISTED\|ACCEPTED\|REJECTED"}` | 200 application | 400, 404, 409 (not allowed) | ApplicationStatusChanged |
| `POST /api/applications/{id}/withdraw` | – | 200 application | 404, 409 | ApplicationStatusChanged |

## matching-service (http://localhost:8084)

| Method and path | Body | Success | Errors |
|---|---|---|---|
| `GET /api/notifications/students/{studentId}` | – | 200 notifications, newest first | – |
| `GET /api/matching/students/{studentId}/recommendations` | – | 200 rule-based recommendations | 404 (no profile yet) |
| `GET /api/analytics/jobs-per-category` | – | 200 `{"category": count}` | – |
| `GET /api/analytics/applications-per-job` | – | 200 `{"jobId": count}` | – |
| `GET /api/analytics/trending-jobs` | – | 200 `[{"jobId", "title", "applicationsInWindow"}]` | – |
| `GET /api/analytics/source` | – | 200 `{"source": "in-memory counters" or "kafka streams"}` | – |
| `GET /api/ai/status` | – | 200 `{"enabled": true\|false}` | – |
| `GET /api/ai/students/{studentId}/recommendations` | – | 200 `{"source": "AI_AGENT\|RULE_BASED_FALLBACK", "note", "recommendations": [..]}` | 404, 503 |
| `POST /api/ai/job-ad/review` | `{"title", "category", "location", "pay", "description"}` | 200 `{"improvedTitle", "improvedDescription", "tips", "revisions", "reviewNotes"}` | 502, 503 |

## Kafka topics

| Topic | Event | Publisher | Consumer |
|---|---|---|---|
| `student-profile-updated` | StudentProfileUpdated | user-service | matching-service |
| `job-posted` | JobPosted | job-service | matching-service (and the Kafka Streams query for R11) |
| `job-updated` | JobUpdated | job-service | matching-service |
| `job-closed` | JobClosed | job-service | matching-service |
| `application-submitted` | ApplicationSubmitted | application-service | matching-service (and the Kafka Streams queries for R12) |
| `application-status-changed` | ApplicationStatusChanged | application-service | matching-service |
