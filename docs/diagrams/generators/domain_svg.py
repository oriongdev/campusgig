"""UML domain model of CampusGig grouped into sub-domains, laid out by hand.

Run: python3 domain_svg.py <out dir>
Writes domain-model.svg (the whole model) and one close-up per sub-domain.
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from svgkit import Box, panel, path, elbow, document, INK, HEAD

PANEL_BG = "#F3F7FF"


def build(only=None):
    """Returns (svg body, view box). only = None for the whole model, or one of user / job / app / match."""
    body, B, E, owner = [], {}, [], {}
    full = only is None

    def box(ctx, key, x, y, st, name, attrs=(), ops=(), min_w=0):
        B[key] = Box(key, x, y, st, name, attrs, ops, min_w)
        owner[key] = ctx
        return B[key]

    def rel(ctx, points, kind, label=None, **kw):
        """ctx is the sub-domain a relationship lives in, or None when it crosses sub-domains."""
        if full or ctx == only:
            E.append(path(points, kind, label, **kw))

    def caption(x, y, text):
        if full or only == "match":
            body.append(f'<text x="{x}" y="{y}" font-size="11" font-weight="700" fill="#64748B" letter-spacing="0.8">{text}</text>')

    # ------------------------------------------------------------------ panels
    match_w, match_h = (620, 1222) if full else (550, 925)
    panels = {
        "user": (20, 20, 940, 372, "User Management", "supporting sub-domain · bounded context: user-service", "#F7F9FC", 330),
        "job": (20, 412, 940, 420, "Job Listings", "supporting sub-domain · bounded context: job-service", "#F7F9FC", 330),
        "app": (20, 852, 940, 390, "Applications", "core sub-domain · bounded context: application-service", PANEL_BG, 330),
        "match": (990, 20, match_w, match_h, "Matching, Notifications and Analytics",
                  "core sub-domain · bounded context: matching-service", PANEL_BG, 16),
    }
    for key, (x, y, w, h, title, sub, fill, tx) in panels.items():
        if full or key == only:
            # In the whole model the left-hand titles sit clear of the lines that cross between sub-domains.
            body.append(panel(x, y, w, h, title, sub, fill, tx if full else 16))

    # ------------------------------------------------------------------ user management
    box("user", "Student", 45, 105, "aggregate root", "Student",
        ["id: Long", "name: String", "email: EmailAddress", "skills: Set<String>", "availability: Availability"],
        ["updateProfile(skills, availability)"])
    box("user", "Availability", 350, 105, "enumeration", "Availability", ["WEEKDAYS", "WEEKENDS", "EVENINGS", "FLEXIBLE"], min_w=140)
    box("user", "EmailAddress", 350, 262, "value object", "EmailAddress", ["value: String"], ["trimmed, lower-cased, validated"])
    box("user", "Employer", 640, 262, "aggregate root", "Employer", ["id: Long", "companyName: String", "email: EmailAddress"], min_w=170)
    box("user", "StudentProfileUpdated", 715, 105, "domain event", "StudentProfileUpdated",
        ["studentId, name, skills,", "availability, occurredAt"])

    # ------------------------------------------------------------------ job listings
    box("job", "Job", 45, 482, "aggregate root", "Job",
        ["id: Long", "employerId: Long", "title, description: String", "category, location: String", "pay: Pay",
         "status: JobStatus", "postedAt, closedAt: Instant"],
        ["update(title, description, ...)", "close()", "isOpen(): boolean", "matches(category, location, minPay)"])
    box("job", "Pay", 350, 497, "value object", "Pay", ["amount: BigDecimal", "type: PayType"], ["amount must be above zero"])
    box("job", "PayType", 565, 497, "enumeration", "PayType", ["HOURLY", "FIXED"], min_w=100)
    box("job", "JobStatus", 350, 655, "enumeration", "JobStatus", ["OPEN", "CLOSED"], min_w=110)
    box("job", "JobPosted", 715, 497, "domain event", "JobPosted",
        ["jobId, employerId, title,", "description, category, location,", "payAmount, payType, occurredAt"])
    box("job", "JobUpdated", 715, 625, "domain event", "JobUpdated", ["jobId and the new details"], min_w=225)
    box("job", "JobClosed", 715, 720, "domain event", "JobClosed", ["jobId, occurredAt"], min_w=225)

    # ------------------------------------------------------------------ applications
    box("app", "JobApplication", 45, 940, "aggregate root", "JobApplication",
        ["id: Long", "jobId: Long", "studentId: Long", "coverMessage: String", "status: ApplicationStatus",
         "submittedAt, updatedAt: Instant"],
        ["moveTo(next): ApplicationStatus"])
    box("app", "ApplicationStatus", 350, 940, "enumeration", "ApplicationStatus",
        ["SUBMITTED", "SHORTLISTED", "ACCEPTED", "REJECTED", "WITHDRAWN"], ["canMoveTo(next): boolean"])
    box("app", "ApplicationSubmitted", 715, 940, "domain event", "ApplicationSubmitted",
        ["applicationId, jobId,", "studentId, occurredAt"], min_w=225)
    box("app", "ApplicationStatusChanged", 715, 1070, "domain event", "ApplicationStatusChanged",
        ["applicationId, jobId, studentId,", "oldStatus, newStatus, occurredAt"])

    # ------------------------------------------------------------------ matching, notifications and analytics
    # The two columns of the right-hand panel. The whole model leaves room on the left for event labels.
    LX, RX = (1085, 1362) if full else (1015, 1292)
    caption(LX, 92, "MATCHING")
    box("match", "StudentSnapshot", LX, 105, "read model", "StudentSnapshot",
        ["studentId: Long", "name: String", "skills: Set<String>", "availability: String"], ["update(name, skills, availability)"])
    box("match", "JobSnapshot", LX, 300, "read model", "JobSnapshot",
        ["jobId: Long", "title, description: String", "category, location: String"], min_w=238)
    box("match", "SkillMatcher", RX, 105, "domain service", "SkillMatcher", (), ["match(student, job):", "   Optional<JobRecommendation>"])
    box("match", "JobRecommendation", RX, 235, "value object", "JobRecommendation",
        ["jobId, title, category", "score: double (0 to 1)", "reason: String"], min_w=223)

    caption(LX, 472, "RESULTS OF THE AI AGENTS")
    box("match", "AiRecommendationResult", LX, 485, "value object", "AiRecommendationResult",
        ["source: AI_AGENT or", "   RULE_BASED_FALLBACK", "note: String", "recommendations (0 to 5):", "   jobId, title, score, reason"], min_w=238)
    box("match", "JobAdGuard", RX, 485, "domain service", "JobAdGuard", (), ["check(draft, title, description):", "   List<String> problems"])
    box("match", "JobAdReview", RX, 615, "value object", "JobAdReview",
        ["improvedTitle: String", "improvedDescription: String", "tips: List<String>", "revisions: int", "reviewNotes: List<String>"], min_w=223)

    if full:   # lined up with the events that feed them
        caption(LX, 927, "ANALYTICS")
        box("match", "TrendingJob", LX, 940, "value object", "TrendingJob", ["jobId, title", "applicationsInWindow: long"], min_w=238)
        caption(LX, 1057, "NOTIFICATIONS")
        box("match", "Notification", LX, 1070, "aggregate root", "Notification",
            ["id: Long", "studentId: Long", "message: String", "createdAt: Instant"], min_w=238)
    else:      # the close-up has no events to line up with, so it is packed tighter
        caption(LX, 787, "ANALYTICS")
        box("match", "TrendingJob", LX, 800, "value object", "TrendingJob", ["jobId, title", "applicationsInWindow: long"], min_w=238)
        caption(RX, 787, "NOTIFICATIONS")
        box("match", "Notification", RX, 800, "aggregate root", "Notification",
            ["id: Long", "studentId: Long", "message: String", "createdAt: Instant"], min_w=223)

    # ------------------------------------------------------------------ relationships inside a sub-domain
    s, ev, j, ja = B["Student"], B["StudentProfileUpdated"], B["Job"], B["JobApplication"]
    rel("user", elbow(s, "r", B["Availability"], "l", fa=0.3, fb=0.5), "uses")
    rel("user", elbow(s, "r", B["EmailAddress"], "l", mid=320, fa=0.85, fb=0.35), "compose")
    y = B["EmailAddress"].anchor("r", 0.5)[1]
    rel("user", [(B["Employer"].x, y), (B["EmailAddress"].x + B["EmailAddress"].w, y)], "compose")
    rel("user", [s.anchor("t", 0.8), (s.anchor("t", 0.8)[0], 86), (ev.anchor("t")[0], 86), ev.anchor("t")], "event",
        "publishes", label_at=(300, 81))

    rel("job", elbow(j, "r", B["Pay"], "l", fa=0.2, fb=0.45), "compose")
    rel("job", elbow(B["Pay"], "r", B["PayType"], "l", fa=0.4, fb=0.5), "uses")
    rel("job", elbow(j, "r", B["JobStatus"], "l", fa=0.85, fb=0.5), "uses")
    bus_y, trunk_x = 472, 695
    start = j.anchor("t", 0.85)
    for key in ("JobPosted", "JobUpdated", "JobClosed"):
        target = B[key].anchor("l")
        rel("job", [start, (start[0], bus_y), (trunk_x, bus_y), (trunk_x, target[1]), target], "event",
            "publishes" if key == "JobPosted" else None, label_at=(672, bus_y - 5))

    rel("app", elbow(ja, "r", B["ApplicationStatus"], "l", fa=0.5, fb=0.5), "uses")
    bus_y = 922
    start = ja.anchor("t", 0.85)
    for key in ("ApplicationSubmitted", "ApplicationStatusChanged"):
        target = B[key].anchor("l")
        rel("app", [start, (start[0], bus_y), (trunk_x, bus_y), (trunk_x, target[1]), target], "event",
            "publishes" if key == "ApplicationSubmitted" else None, label_at=(672, bus_y - 5))

    sm, js, ss, g = B["SkillMatcher"], B["JobSnapshot"], B["StudentSnapshot"], B["JobAdGuard"]
    y = sm.anchor("l", 0.5)[1]
    rel("match", [(sm.x, y), (ss.x + ss.w, y)], "uses", "reads", label_at=((sm.x + ss.x + ss.w) / 2, y - 6), bg=PANEL_BG)
    rel("match", elbow(sm, "b", B["JobRecommendation"], "t"), "uses", "creates",
        label_at=(sm.anchor("b")[0] + 30, sm.y + sm.h + 26), bg=PANEL_BG)
    p = sm.anchor("l", 0.82)
    rel("match", [p, (RX - 20, p[1]), (RX - 20, js.anchor("r")[1]), js.anchor("r")], "uses", "reads",
        label_at=(RX - 20, 290), bg=PANEL_BG)
    rel("match", elbow(g, "b", B["JobAdReview"], "t"), "uses", "checks",
        label_at=(g.anchor("b")[0] + 28, g.y + g.h + 26), bg=PANEL_BG)

    # ------------------------------------------------------------------ relationships between sub-domains
    # references by identity only
    p = ja.anchor("t", 0.3)
    rel(None, [p, (p[0], j.y + j.h)], "byid", "jobId", label_at=(p[0] + 26, 846))
    p = ja.anchor("l", 0.25); q = s.anchor("l", 0.5)
    rel(None, [p, (9, p[1]), (9, q[1]), q], "byid", "studentId", label_at=(14, 406), anchor="start")
    p = j.anchor("t", 0.3); q = B["Employer"].anchor("b", 0.5)
    rel(None, [p, (p[0], 402), (q[0], 402), q], "byid", "employerId", label_at=(440, 397))

    # events consumed by matching-service; the labels sit just inside the right-hand panel
    LABEL_X = 998
    p = ev.anchor("r"); q = (ss.x, p[1])
    rel(None, [p, q], "event", "builds", label_at=(LABEL_X, p[1] - 5), anchor="start", bg=PANEL_BG)
    for key, frac, mid, label in (("JobPosted", 0.25, 966, "builds"), ("JobUpdated", 0.55, 975, "replaces"), ("JobClosed", 0.85, 984, "removes")):
        p = B[key].anchor("r"); q = js.anchor("l", frac)
        rel(None, [p, (mid, p[1]), (mid, q[1]), q], "event", label, label_at=(LABEL_X, q[1] - 5), anchor="start", bg=PANEL_BG)
    p = B["ApplicationSubmitted"].anchor("r")
    rel(None, [p, (B["TrendingJob"].x, p[1])], "event", "counted into", label_at=(LABEL_X, p[1] - 5), anchor="start", bg=PANEL_BG)
    p = B["ApplicationStatusChanged"].anchor("r")
    rel(None, [p, (B["Notification"].x, p[1])], "event", "creates", label_at=(LABEL_X, p[1] - 5), anchor="start", bg=PANEL_BG)

    body += E
    body += [b.svg() for k, b in B.items() if full or owner[k] == only]

    # ------------------------------------------------------------------ key
    if full:
        lx, ly = RX, 940
        body.append(f'<rect x="{lx}" y="{ly}" width="223" height="248" rx="6" fill="#FFFFFF" stroke="#B8C4D6"/>')
        body.append(f'<text x="{lx + 12}" y="{ly + 22}" font-size="12" font-weight="700">Key</text>')
        rows = [("compose", "composition (part of)"), ("uses", "uses or creates"), ("byid", "refers to by id only"), ("event", "domain event flow")]
        for i, (kind, text) in enumerate(rows):
            y = ly + 46 + i * 24
            body.append(path([(lx + 14, y), (lx + 60, y)], kind))
            body.append(f'<text x="{lx + 70}" y="{y + 4}" font-size="11.5">{text}</text>')
        for i, st in enumerate(["aggregate root", "value object", "read model", "domain service", "domain event"]):
            y = ly + 140 + i * 21
            body.append(f'<rect x="{lx + 14}" y="{y - 11}" width="22" height="14" fill="{HEAD[st]}" stroke="{INK}" stroke-width="1"/>')
            body.append(f'<text x="{lx + 46}" y="{y}" font-size="11.5">{st}</text>')
        return body, (0, 0, 1630, 1262)
    x, y, w, h = panels[only][:4]
    return body, (x - 10, y - 10, w + 20, h + 20)


OUTPUTS = {
    None: ("domain-model", "CampusGig domain model grouped into four sub-domains"),
    "user": ("domain-user-management", "Domain model of the User Management sub-domain"),
    "job": ("domain-job-listings", "Domain model of the Job Listings sub-domain"),
    "app": ("domain-applications", "Domain model of the Applications sub-domain"),
    "match": ("domain-matching", "Domain model of the Matching, Notifications and Analytics sub-domain"),
}

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for only, (name, title) in OUTPUTS.items():
        body, (x, y, w, h) = build(only)
        open(os.path.join(out, name + ".svg"), "w").write(document(w, h, body, title, origin=(x, y)))
    print("domain model written:", ", ".join(n for n, _ in OUTPUTS.values()))
