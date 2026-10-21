"""CampusGig architecture diagram (services, databases, Kafka topics, external LLM). Run: python3 arch_svg.py <out dir>"""
import html, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from svgkit import path, document, INK, tw

W, H = 1480, 772
TEAL, TEAL_BG = "#0E7490", "#ECF8F6"
body = []

def text(x, y, s, size=12, weight=400, fill=INK, anchor="start"):
    body.append(f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" fill="{fill}" text-anchor="{anchor}">{html.escape(s)}</text>')

def cylinder(x, y, w, h, label):
    ry = 7
    body.append(f'<path d="M {x} {y + ry} A {w / 2} {ry} 0 0 1 {x + w} {y + ry} V {y + h - ry} A {w / 2} {ry} 0 0 1 {x} {y + h - ry} Z" '
                f'fill="#FFF7DB" stroke="{INK}" stroke-width="1.2"/>')
    body.append(f'<path d="M {x} {y + ry} A {w / 2} {ry} 0 0 0 {x + w} {y + ry}" fill="none" stroke="{INK}" stroke-width="1.2"/>')
    text(x + w / 2, y + h / 2 + 8, label, 11.5, anchor="middle")

SERVICE_Y, SERVICE_H, SERVICE_W = 190, 204, 290
def service(x, name, port, lines, db):
    y, w, h = SERVICE_Y, SERVICE_W, SERVICE_H
    body.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="#FFFFFF" stroke="{INK}" stroke-width="1.4"/>')
    body.append(f'<path d="M {x} {y + 44} V {y + 8} a 8 8 0 0 1 8 -8 H {x + w - 8} a 8 8 0 0 1 8 8 V {y + 44} Z" fill="#DBEAFE" stroke="{INK}" stroke-width="1.4"/>')
    text(x + 14, y + 19, "«microservice» Spring Boot", 10.5, fill="#33425C")
    text(x + 14, y + 36, name, 14.5, 700)
    text(x + w - 14, y + 36, ":" + port, 13, 600, anchor="end")
    for i, line in enumerate(lines):
        text(x + 14, y + 66 + i * 18, line)
    cylinder(x + 14, y + h - 50, 206, 38, "H2 in memory: " + db)

# ------------------------------------------------------------------ clients and the external LLM
body.append(f'<rect x="40" y="30" width="1210" height="70" rx="8" fill="#F7F9FC" stroke="#8494AB" stroke-width="1.2"/>')
text(58, 58, "Clients", 14.5, 700)
text(58, 80, "Web page (index.html, served by user-service on http://localhost:8081)   ·   Postman collection   ·   demo.sh (curl)", 12)
body.append(f'<rect x="1280" y="30" width="160" height="70" rx="8" fill="#FFF4E0" stroke="{INK}" stroke-width="1.2" stroke-dasharray="6 4"/>')
text(1360, 58, "Google Gemini API", 13.5, 700, anchor="middle")
text(1360, 80, "external LLM", 12, anchor="middle")

# ------------------------------------------------------------------ services
XS = {"user": 40, "job": 370, "application": 820, "matching": 1150}
service(XS["user"], "user-service", "8081",
        ["Students and employers", "Aggregates: Student, Employer", "Also serves the web page"], "userdb")
service(XS["job"], "job-service", "8082",
        ["Job listings and search", "Aggregate: Job (with Pay)", "Answers “is this job open?”"], "jobdb")
service(XS["application"], "application-service", "8083",
        ["Job applications and their status", "Aggregate: JobApplication", "Asks job-service before accepting"], "applicationdb")
service(XS["matching"], "matching-service", "8084",
        ["Matching, notifications, analytics", "Read models built from events", "Two LangChain4j AI agents", "Kafka Streams (streams profile)"], "matchingdb")
cx = {k: x + SERVICE_W / 2 for k, x in XS.items()}
bottom = SERVICE_Y + SERVICE_H

# ------------------------------------------------------------------ Kafka
KY, KH = 500, 190
body.append(f'<rect x="40" y="{KY}" width="1400" height="{KH}" rx="10" fill="{TEAL_BG}" stroke="{TEAL}" stroke-width="1.4"/>')
text(1170, KY + 70, "Apache Kafka", 15.5, 700)
text(1170, KY + 93, "one broker on localhost:9092", 12)
text(1170, KY + 111, "six topics, shown under the", 12)
text(1170, KY + 129, "service that publishes to them", 12)
text(1170, KY + 147, "one event type per topic, as JSON", 12)

def topics(x, names):
    """A column of topic pills under the service that publishes to them."""
    top = KY + 28
    h = len(names) * 40 + 12
    body.append(f'<rect x="{x}" y="{top}" width="250" height="{h}" rx="8" fill="#FFFFFF" stroke="{TEAL}" stroke-width="1" stroke-dasharray="4 3"/>')
    for i, name in enumerate(names):
        y = top + 10 + i * 40
        body.append(f'<rect x="{x + 12}" y="{y}" width="226" height="30" rx="15" fill="#D9F4EF" stroke="{TEAL}" stroke-width="1.2"/>')
        text(x + 125, y + 20, name, 12.5, 600, anchor="middle")
    return top

tops = {
    "user": topics(XS["user"] + 20, ["student-profile-updated"]),
    "job": topics(XS["job"] + 20, ["job-posted", "job-updated", "job-closed"]),
    "application": topics(XS["application"] + 20, ["application-submitted", "application-status-changed"]),
}

# ------------------------------------------------------------------ connectors
E = []
for key in ("user", "job", "application"):          # clients call each service
    E.append(path([(cx[key], 100), (cx[key], SERVICE_Y)], "call", "REST / JSON", label_at=(cx[key] + 8, 150), anchor="start"))
E.append(path([(1200, 100), (1200, SERVICE_Y)], "call", "REST / JSON", label_at=(1192, 150), anchor="end"))
E.append(path([(1380, SERVICE_Y), (1380, 100)], "call"))
for i, line in enumerate(["HTTPS, through", "LangChain4j"]):
    E.append(f'<text x="1372" y="{143 + i * 15}" text-anchor="end" font-size="11" fill="#33425C">{line}</text>')

y = SERVICE_Y + 110                                   # the one synchronous call between services
E.append(path([(XS["application"], y), (XS["job"] + SERVICE_W, y)], "call"))
mid = (XS["application"] + XS["job"] + SERVICE_W) / 2
E.append(f'<text x="{mid}" y="{y - 26}" text-anchor="middle" font-size="11" font-weight="700" fill="#33425C">synchronous REST</text>')
E.append(f'<text x="{mid}" y="{y - 10}" text-anchor="middle" font-size="11" fill="#33425C">GET /api/jobs/{{id}}</text>')
E.append(f'<text x="{mid}" y="{y + 18}" text-anchor="middle" font-size="11" fill="#33425C">before an application</text>')
E.append(f'<text x="{mid}" y="{y + 33}" text-anchor="middle" font-size="11" fill="#33425C">is accepted</text>')

for key in ("user", "job", "application"):          # events out
    E.append(path([(cx[key], bottom), (cx[key], tops[key])], "flow", "publishes events", label_at=(cx[key] + 8, bottom + 46), anchor="start"))
E.append(path([(cx["matching"], KY), (cx["matching"], bottom)], "flow"))                 # events in
for i, line in enumerate(["consumes all six topics", "(consumer group matching-service)"]):
    E.append(f'<text x="{cx["matching"] - 8}" y="{bottom + 40 + i * 15}" text-anchor="end" font-size="11" fill="{TEAL}">{line}</text>')

body += E

# ------------------------------------------------------------------ key
kx, ky = 44, KY + KH + 30
body.append(path([(kx, ky), (kx + 46, ky)], "call"));  text(kx + 56, ky + 4, "synchronous request and response", 11.5)
body.append(path([(kx + 300, ky), (kx + 346, ky)], "flow")); text(kx + 356, ky + 4, "asynchronous domain event", 11.5)
text(1440, ky + 4, "Each service owns its own database. No service reads another service's tables.", 11.5, fill="#33425C", anchor="end")
text(kx, ky + 30, "The streams profile adds two Kafka Streams topologies inside matching-service that read job-posted and application-submitted.", 11.5, fill="#33425C")

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    open(os.path.join(out, "architecture.svg"), "w").write(document(W, H, body, "CampusGig microservices architecture"))
    print("architecture written")
