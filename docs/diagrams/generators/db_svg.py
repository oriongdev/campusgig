"""CampusGig database design: one H2 database per service, with the tables Hibernate creates from the entities.
Run: python3 db_svg.py <out dir>"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from svgkit import Box, panel, path, document, INK

W, H = 1500, 800
body, B = [], {}
def table(key, x, y, cols, kind="table", min_w=300):
    B[key] = Box(key, x, y, kind, key, cols, (), min_w)
    return B[key]

body.append(panel(20, 20, 720, 360, "userdb", "owned by user-service"))
body.append(panel(760, 20, 720, 360, "jobdb", "owned by job-service"))
body.append(panel(20, 400, 720, 360, "applicationdb", "owned by application-service"))
body.append(panel(760, 400, 720, 360, "matchingdb", "owned by matching-service · yellow tables are copies built from Kafka events"))

table("student", 45, 90, ["PK id: BIGINT", "name: VARCHAR", "email: VARCHAR, not null", "availability: VARCHAR (enum)"])
table("student_skills", 45, 250, ["FK student_id → student.id", "skill: VARCHAR"])
table("employer", 400, 90, ["PK id: BIGINT", "company_name: VARCHAR", "email: VARCHAR, not null"])

table("job", 785, 90, ["PK id: BIGINT", "employer_id: BIGINT  (an id in userdb)", "title: VARCHAR", "description: VARCHAR(2000)", "category: VARCHAR",
                       "location: VARCHAR", "pay_amount: DECIMAL, not null", "pay_type: VARCHAR, not null", "status: VARCHAR (enum)",
                       "posted_at: TIMESTAMP", "closed_at: TIMESTAMP"], min_w=340)

table("job_application", 45, 470, ["PK id: BIGINT", "job_id: BIGINT  (an id in jobdb)", "student_id: BIGINT  (an id in userdb)",
                                   "cover_message: VARCHAR(2000)", "status: VARCHAR (enum)", "submitted_at: TIMESTAMP", "updated_at: TIMESTAMP"], min_w=340)

table("student_snapshot", 785, 470, ["PK student_id: BIGINT", "name: VARCHAR", "availability: VARCHAR"], kind="copy", min_w=320)
table("student_snapshot_skills", 785, 610, ["FK student_id → student_snapshot", "skill: VARCHAR"], kind="copy", min_w=320)
table("job_snapshot", 1130, 470, ["PK job_id: BIGINT", "title: VARCHAR", "description: VARCHAR(2000)", "category: VARCHAR", "location: VARCHAR"], kind="copy", min_w=320)
table("notification", 1130, 625, ["PK id: BIGINT", "student_id: BIGINT  (an id in userdb)", "message: VARCHAR(500)", "created_at: TIMESTAMP"], min_w=320)

for child, parent in (("student_skills", "student"), ("student_snapshot_skills", "student_snapshot")):
    c, p = B[child], B[parent]
    x = c.x + 60
    body.append(path([(x, c.y), (x, p.y + p.h)], "uses", "FK", label_at=(x + 22, (c.y + p.y + p.h) / 2 + 4), bg="#F7F9FC"))

body += [b.svg() for b in B.values()]
body.append('<text x="20" y="788" font-size="12.5" fill="#33425C">No foreign key crosses a database: an id that belongs to another service is a plain column, '
            'and the copies in matchingdb are kept up to date from events, never by reading another database.</text>')

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    open(os.path.join(out, "database.svg"), "w").write(document(W, H, body, "CampusGig database design, one database per service"))
    print("database written")
