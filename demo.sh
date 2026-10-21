#!/usr/bin/env bash
# Walks through all 14 functional requirements against the running services and checks every
# response. Start the system first with ./run.sh (or ./run.sh --streams), then run ./demo.sh.
#
# It only needs curl. It registers new users each time, so it can be run again without restarting.
# The output is also the sample input and output for each user story.

USERS=http://localhost:8081
JOBS=http://localhost:8082
APPS=http://localhost:8083
MATCH=http://localhost:8084

pass=0
fail=0
RESPONSE=""
RUN=$(date +%s)

heading() { printf '\n\033[1m== %s\033[0m\n' "$*"; }

ok() { pass=$((pass + 1)); }
bad() { fail=$((fail + 1)); printf '   \033[31mFAILED: %s\033[0m\n' "$*"; }

# call <expected status> <method> <url> [json body]
call() {
  expected=$1; method=$2; url=$3; body=${4:-}
  echo "$method $url"
  if [ -n "$body" ]; then
    echo "   $body"
    out=$(curl -s -w '\n%{http_code}' -X "$method" "$url" -H 'Content-Type: application/json' -d "$body")
  else
    out=$(curl -s -w '\n%{http_code}' -X "$method" "$url")
  fi
  code=${out##*$'\n'}
  RESPONSE=${out%$'\n'*}
  echo "   -> $code $RESPONSE"
  if [ "$code" = "$expected" ]; then ok; else bad "expected HTTP $expected"; fi
}

# The id of the thing that was just created.
new_id() { echo "$RESPONSE" | sed -E 's/^\{"id":([0-9]+).*/\1/'; }

contains() {
  case "$RESPONSE" in
    *"$1"*) ok ;;
    *) bad "expected the response to contain: $1" ;;
  esac
}

lacks() {
  case "$RESPONSE" in
    *"$1"*) bad "expected the response not to contain: $1" ;;
    *) ok ;;
  esac
}

# Events travel through Kafka, so results appear a moment later. Poll until they do.
# eventually <url> <text that must appear>
eventually() {
  for _ in $(seq 1 30); do
    RESPONSE=$(curl -s "$1")
    case "$RESPONSE" in *"$2"*) echo "GET $1"; echo "   -> 200 $RESPONSE"; ok; return 0 ;; esac
    sleep 1
  done
  echo "GET $1"; echo "   -> $RESPONSE"
  bad "waited 30 seconds for: $2"
}

for port in 8081 8082 8083 8084; do
  if ! (echo > /dev/tcp/127.0.0.1/$port) >/dev/null 2>&1; then
    echo "Nothing is listening on port $port. Start the system with ./run.sh first."
    exit 1
  fi
done

heading "1. Student registers a profile with skills and availability"
call 201 POST $USERS/api/students "{\"name\":\"Alex Nguyen\",\"email\":\"alex.$RUN@uowmail.edu.au\",\"skills\":[\"barista\",\"customer service\"],\"availability\":\"WEEKENDS\"}"
ALEX=$(new_id)
call 201 POST $USERS/api/students "{\"name\":\"Priya Shah\",\"email\":\"priya.$RUN@uowmail.edu.au\",\"skills\":[\"java\",\"tutoring\"],\"availability\":\"EVENINGS\"}"
PRIYA=$(new_id)
echo "-- an invalid email is rejected"
call 400 POST $USERS/api/students '{"name":"No Email","email":"not-an-email","skills":[],"availability":"FLEXIBLE"}'

heading "2. Employer registers an account"
call 201 POST $USERS/api/employers "{\"companyName\":\"Northbeach Cafe\",\"email\":\"jobs.$RUN@northbeachcafe.com.au\"}"
EMPLOYER=$(new_id)
echo "-- the same email cannot register twice"
call 409 POST $USERS/api/employers "{\"companyName\":\"Northbeach Cafe\",\"email\":\"jobs.$RUN@northbeachcafe.com.au\"}"

heading "3. Student updates their profile (publishes StudentProfileUpdated)"
call 200 PUT $USERS/api/students/$ALEX/profile '{"skills":["barista","customer service","retail"],"availability":"WEEKENDS"}'
contains '"retail"'

heading "4. Employer posts a job (publishes JobPosted)"
call 201 POST $JOBS/api/jobs "{\"employerId\":$EMPLOYER,\"title\":\"Weekend Barista\",\"description\":\"Make coffee and handle customer service.\",\"category\":\"hospitality\",\"location\":\"Wollongong\",\"payAmount\":28.50,\"payType\":\"HOURLY\"}"
BARISTA=$(new_id)
call 201 POST $JOBS/api/jobs "{\"employerId\":$EMPLOYER,\"title\":\"Java Tutor\",\"description\":\"Tutoring first-year students in Java.\",\"category\":\"tutoring\",\"location\":\"UOW Campus\",\"payAmount\":40,\"payType\":\"HOURLY\"}"
TUTOR=$(new_id)
echo "-- pay must be greater than zero"
call 400 POST $JOBS/api/jobs "{\"employerId\":$EMPLOYER,\"title\":\"Unpaid Intern\",\"description\":\"\",\"category\":\"office\",\"location\":\"Wollongong\",\"payAmount\":0,\"payType\":\"HOURLY\"}"

heading "5. Employer edits a job (publishes JobUpdated)"
call 200 PUT $JOBS/api/jobs/$BARISTA "{\"employerId\":$EMPLOYER,\"title\":\"Weekend Barista\",\"description\":\"Make coffee, handle customer service and close up on Sundays.\",\"category\":\"hospitality\",\"location\":\"North Wollongong\",\"payAmount\":30,\"payType\":\"HOURLY\"}"
contains '"location":"North Wollongong"'

heading "6. Student searches jobs by category, pay or location"
call 200 GET "$JOBS/api/jobs?category=hospitality&minPay=25"
contains "\"id\":$BARISTA,"
call 200 GET "$JOBS/api/jobs?location=campus"
contains "\"id\":$TUTOR,"
call 200 GET "$JOBS/api/jobs?minPay=35"
lacks "\"id\":$BARISTA,"

heading "7. Student applies with a cover message (publishes ApplicationSubmitted)"
call 201 POST $APPS/api/applications "{\"jobId\":$BARISTA,\"studentId\":$ALEX,\"coverMessage\":\"I have two years of barista experience.\"}"
ALEX_BARISTA=$(new_id)
call 201 POST $APPS/api/applications "{\"jobId\":$BARISTA,\"studentId\":$PRIYA,\"coverMessage\":\"I can work Sundays.\"}"
PRIYA_BARISTA=$(new_id)
call 201 POST $APPS/api/applications "{\"jobId\":$TUTOR,\"studentId\":$PRIYA,\"coverMessage\":\"I tutor Java already.\"}"
PRIYA_TUTOR=$(new_id)
echo "-- a student cannot apply for the same job twice"
call 409 POST $APPS/api/applications "{\"jobId\":$BARISTA,\"studentId\":$ALEX,\"coverMessage\":\"Me again.\"}"
echo "-- application-service asks job-service whether the job exists"
call 404 POST $APPS/api/applications "{\"jobId\":999999,\"studentId\":$ALEX,\"coverMessage\":\"Hello?\"}"

heading "8. Student withdraws an application"
call 200 POST $APPS/api/applications/$PRIYA_TUTOR/withdraw
contains '"status":"WITHDRAWN"'

heading "9. Employer shortlists, accepts or rejects an applicant (publishes ApplicationStatusChanged)"
call 200 GET "$APPS/api/applications?jobId=$BARISTA"
call 200 PUT $APPS/api/applications/$ALEX_BARISTA/status '{"status":"SHORTLISTED"}'
call 200 PUT $APPS/api/applications/$ALEX_BARISTA/status '{"status":"ACCEPTED"}'
call 200 PUT $APPS/api/applications/$PRIYA_BARISTA/status '{"status":"REJECTED"}'
echo "-- an accepted application is final"
call 409 PUT $APPS/api/applications/$ALEX_BARISTA/status '{"status":"REJECTED"}'

heading "10. Student is notified when their application status changes (arrives through Kafka)"
eventually $MATCH/api/notifications/students/$ALEX 'is now ACCEPTED'
contains 'is now SHORTLISTED'
eventually $MATCH/api/notifications/students/$PRIYA 'is now REJECTED'

heading "11. Real-time: live count of jobs posted per category"
call 200 GET $MATCH/api/analytics/source
eventually $MATCH/api/analytics/jobs-per-category '"tutoring"'
contains '"hospitality"'

heading "12. Real-time: live applications per job, and trending jobs"
eventually $MATCH/api/analytics/applications-per-job "\"$BARISTA\":2"
contains "\"$TUTOR\":1"
eventually $MATCH/api/analytics/trending-jobs "\"jobId\":$BARISTA,"

heading "13. Matching agent recommends jobs for a student"
echo "-- rule-based matcher (always available)"
eventually $MATCH/api/matching/students/$ALEX/recommendations 'Weekend Barista'
call 200 GET $MATCH/api/ai/status
case "$RESPONSE" in
  *true*)
    AI=on
    echo "-- AI matching agent (Gemini through LangChain4j, uses tools to look up jobs)"
    call 200 GET $MATCH/api/ai/students/$ALEX/recommendations
    contains '"source":"AI_AGENT"'
    ;;
  *)
    AI=off
    echo "-- GEMINI_API_KEY is not set, so the AI agent answers 503"
    call 503 GET $MATCH/api/ai/students/$ALEX/recommendations
    ;;
esac

heading "14. Job ad assistant improves an employer's draft ad"
DRAFT='{"title":"barista wanted","category":"hospitality","location":"Wollongong","pay":"28.50 HOURLY","description":"need someone for weekends. must be good with people"}'
if [ "$AI" = on ]; then
  call 200 POST $MATCH/api/ai/job-ad/review "$DRAFT"
  contains '"improvedDescription"'
else
  call 503 POST $MATCH/api/ai/job-ad/review "$DRAFT"
fi

heading "5 (continued). Employer closes a job (publishes JobClosed)"
call 200 POST $JOBS/api/jobs/$TUTOR/close
contains '"status":"CLOSED"'
echo "-- a closed job takes no more applications"
call 409 POST $APPS/api/applications "{\"jobId\":$TUTOR,\"studentId\":$ALEX,\"coverMessage\":\"Too late?\"}"
echo "-- and is no longer offered in search"
call 200 GET "$JOBS/api/jobs?category=tutoring"
lacks "\"id\":$TUTOR,"

echo
if [ "$fail" -eq 0 ]; then
  printf '\033[32mAll %d checks passed.\033[0m' "$pass"
else
  printf '\033[31m%d of %d checks failed.\033[0m' "$fail" $((pass + fail))
fi
[ "$AI" = off ] && printf ' The two AI agents were skipped because GEMINI_API_KEY is not set.'
echo
[ "$fail" -eq 0 ]
