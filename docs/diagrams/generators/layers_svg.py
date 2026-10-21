"""Layered architecture of the four CampusGig services, with the real classes in each layer.
Run: python3 layers_svg.py <out dir>"""
import html, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from svgkit import path, document, INK

W = 1560
LABEL_X, LABEL_W = 30, 180
COL_X, COL_W = [228, 561, 894, 1227], 318
GREY = "#5B6B82"
LINE = 17

SERVICES = ["user-service", "job-service", "application-service", "matching-service"]

# Each cell is a list of lines. "# name" is a sub-package caption. "text | note" prints the note in grey.
LAYERS = [
    ("Presentation", "#DBEAFE",
     ["REST controllers, request and", "response DTOs, error mapping"], "uses: service, domain",
     [["StudentController, EmployerController", "RestExceptionHandler", "# dto", "5 request and response records"],
      ["JobController", "RestExceptionHandler", "# dto", "PostJobRequest, JobResponse"],
      ["ApplicationController", "RestExceptionHandler", "# dto", "SubmitApplicationRequest,", "ChangeStatusRequest, ApplicationResponse"],
      ["MatchingController, NotificationController", "AnalyticsController, AiController", "RestExceptionHandler", "# dto",
       "JobAdRequest, NotificationResponse"]]),
    ("Service", "#E3F4EA",
     ["Use cases, transactions,", "publishing events"], "uses: domain, infrastructure",
     [["UserService"],
      ["JobService"],
      ["ApplicationService"],
      ["MatchingService, NotificationService", "AnalyticsService, AnalyticsQueryService", "AiAgentService"]]),
    ("Domain", "#FDEFC3",
     ["Aggregates, value objects,", "domain services and events"], "uses: no other layer",
     [["Student, Employer | aggregate roots", "EmailAddress | value object", "Availability | enumeration", "# event", "StudentProfileUpdated"],
      ["Job | aggregate root", "Pay | value object", "PayType, JobStatus | enumerations", "# event", "JobPosted, JobUpdated, JobClosed"],
      ["JobApplication | aggregate root", "ApplicationStatus | enumeration", "(holds the allowed status transitions)", "# event",
       "ApplicationSubmitted, ApplicationStatusChanged"],
      ["Notification | aggregate root", "StudentSnapshot, JobSnapshot | read models", "SkillMatcher, JobAdGuard | domain services",
       "JobRecommendation, TrendingJob, JobAdReview,", "AiRecommendationResult | value objects", "# event",
       "the six event records this service consumes"]]),
    ("Infrastructure", "#EFE3FB",
     ["Database, Kafka, REST client,", "LLM, stream processing"], "uses: domain",
     [["# persistence", "StudentRepository, EmployerRepository", "# messaging", "UserEventPublisher", "# config", "WebConfig"],
      ["# persistence", "JobRepository", "# messaging", "JobEventPublisher", "# config", "WebConfig"],
      ["# persistence", "JobApplicationRepository", "# messaging", "JobApplicationEventPublisher", "# client", "JobServiceClient",
       "# config", "WebConfig"],
      ["# persistence", "StudentSnapshotRepository,", "JobSnapshotRepository, NotificationRepository", "# messaging",
       "EventConsumers | six Kafka consumers", "# ai", "GeminiConfig, JobMatchingAgent,", "JobCatalogTools, JobAdAssistant, JobAdCritic",
       "# streams", "StreamProcessors, StreamAnalyticsService,", "JsonSerde and two event serdes", "# config", "WebConfig"]]),
]

body = []
def text(x, y, s, size=12, weight=400, fill=INK, anchor="start", extra=""):
    body.append(f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" fill="{fill}" text-anchor="{anchor}" {extra}>{html.escape(s)}</text>')

# ------------------------------------------------------------------ column headers
y = 30
for x, name in zip(COL_X, SERVICES):
    body.append(f'<rect x="{x}" y="{y}" width="{COL_W}" height="36" rx="6" fill="{INK}"/>')
    text(x + COL_W / 2, y + 23, name, 13.5, 700, "#FFFFFF", "middle")
text(LABEL_X + LABEL_W / 2, y + 23, "Layer (package)", 12, 700, GREY, "middle")

# ------------------------------------------------------------------ rows
y += 54
bounds = []
for name, colour, about, uses, cells in LAYERS:
    h = max(len(c) for c in cells) * LINE + 22
    h = max(h, 96)
    bounds.append((y, h))
    body.append(f'<rect x="{LABEL_X}" y="{y}" width="{LABEL_W}" height="{h}" rx="8" fill="{colour}" stroke="{INK}" stroke-width="1.3"/>')
    text(LABEL_X + 12, y + 24, name, 14.5, 700)
    for i, line in enumerate(about):
        text(LABEL_X + 12, y + 44 + i * 15, line, 11)
    text(LABEL_X + 12, y + 44 + len(about) * 15 + 6, uses, 11, 700, "#33425C")
    for x, lines in zip(COL_X, cells):
        body.append(f'<rect x="{x}" y="{y}" width="{COL_W}" height="{h}" rx="8" fill="#FFFFFF" stroke="{INK}" stroke-width="1.1"/>')
        body.append(f'<rect x="{x}" y="{y}" width="7" height="{h}" rx="3" fill="{colour}" stroke="{INK}" stroke-width="1.1"/>')
        for i, line in enumerate(lines):
            ty = y + 26 + i * LINE
            if line.startswith("# "):
                text(x + 20, ty, line[2:].upper(), 10, 700, GREY, extra='letter-spacing="0.8"')
            elif " | " in line:
                main, note = line.split(" | ")
                body.append(f'<text x="{x + 20}" y="{ty}" font-size="11.5">{html.escape(main)} <tspan fill="{GREY}" font-size="10.5">«{html.escape(note)}»</tspan></text>')
            else:
                text(x + 20, ty, line, 11.5)
    y += h + 30

# ------------------------------------------------------------------ dependency arrows between the layer labels
ax = LABEL_X + LABEL_W / 2
(py, ph), (sy, sh), (dy, dh), (iy, ih) = bounds
body.append(path([(ax, py + ph), (ax, sy)], "uses"))
body.append(path([(ax, sy + sh), (ax, dy)], "uses"))
body.append(path([(ax, iy), (ax, dy + dh)], "uses"))
for yy in (py + ph + 19, sy + sh + 19, dy + dh + 19):
    text(ax + 10, yy, "depends on", 10.5, fill=GREY)

H = y + 28
text(LABEL_X, y + 2, "An arrow points from a layer to the layer it depends on. The service layer also calls the infrastructure layer (repositories, publishers, "
     "the REST client and the AI agents).", 11.5, fill="#33425C")
text(LABEL_X, y + 19, "The one inbound adapter outside the presentation layer is EventConsumers: like a controller, it receives a message and calls the service layer.",
     11.5, fill="#33425C")
H = y + 36

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    open(os.path.join(out, "layered-architecture.svg"), "w").write(document(W, H, body, "Layered architecture of the four CampusGig services"))
    print("layered architecture written")
