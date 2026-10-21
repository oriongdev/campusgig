# Diagrams

The diagrams used in the final report. Each one is here as a PNG (for the report) and an SVG (scales without blurring).

| File | Shows | Report section |
|---|---|---|
| `activity/uc01` to `activity/uc14` | One UML activity diagram per use case, with a swimlane for each actor and service | 2.1 to 2.3 |
| `domain-model` | UML domain model of the whole system, grouped into four sub-domains | 2.4 |
| `domain-user-management`, `domain-job-listings`, `domain-applications`, `domain-matching` | The same model, one sub-domain at a time and easier to read | Not in the report (close-ups of Figure 2.1) |
| `architecture` | The four services, their databases, the six Kafka topics, the one REST call between services, and the LLM | 3.1 |
| `layered-architecture` | The four layers of each service with the real classes in each | 3.2 |
| `database` | The tables in each service's database, and which ones are copies built from events | 3.4 |

## The use cases

| Diagram | Use case |
|---|---|
| uc01 | Student registers a profile |
| uc02 | Employer registers an account |
| uc03 | Student updates their profile |
| uc04 | Employer posts a job |
| uc05 | Employer edits or closes a job |
| uc06 | Student searches for jobs |
| uc07 | Student applies for a job |
| uc08 | Student withdraws an application |
| uc09 | Employer shortlists, accepts or rejects an applicant |
| uc10 | Student is notified when their application status changes |
| uc11 | Real-time analysis: jobs posted per category |
| uc12 | Real-time analysis: applications per job and trending jobs |
| uc13 | AI matching agent recommends jobs to a student |
| uc14 | AI assistant improves an employer's draft job ad |

uc11 and uc12 show the Kafka Streams version of the analytics (`./run.sh --streams`). Without that switch the same events update in-memory counters and the REST endpoints return the same shape of answer.

## How they were made

The diagrams were generated with an AI assistant (see `docs/ai-log.md`) from the scripts in `generators/`, then checked against the code by hand. They are text, so they can be corrected and regenerated when the code changes.

```bash
cd docs/diagrams/generators

# Activity diagrams. Needs Graphviz (brew install graphviz). Writes PNG, SVG and the .dot sources.
python3 usecases.py ../activity

# Domain model, architecture, layers and database. Plain Python, writes SVG.
python3 domain_svg.py ..
python3 arch_svg.py ..
python3 layers_svg.py ..
python3 db_svg.py ..

# Optional: turn those SVGs into PNGs. Needs Node.js and "npm install playwright".
node render.js 1.3 ../domain-*.svg ../architecture.svg ../layered-architecture.svg ../database.svg
```

| Script | What it is |
|---|---|
| `usecases.py` | The 14 use cases, written as steps, decisions and flows |
| `activity.py` | Turns one use case into a Graphviz swimlane diagram |
| `domain_svg.py`, `arch_svg.py`, `layers_svg.py`, `db_svg.py` | The four hand-laid diagrams |
| `svgkit.py` | Shared drawing helpers (class boxes, panels, connectors) |
| `render.js` | SVG to PNG |

If you change a class, an event or an endpoint, change the matching line in the script and run it again. Any SVG can also be opened and edited directly in draw.io, Inkscape or Figma.
