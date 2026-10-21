@echo off
REM Starts Kafka and all four CampusGig services, then opens the web UI.
REM
REM   run.bat            real-time analytics from in-memory counters (default)
REM   run.bat streams    real-time analytics from Kafka Streams
cd /d "%~dp0"

where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven not found. See "Maven on Windows" in README.md for how to install it,
  echo then close and reopen this window. Or run the services from VS Code instead ^(see README^).
  exit /b 1
)

set "MATCHING_PROFILE="
if /i "%~1"=="streams" set "MATCHING_PROFILE=streams"
if /i "%~1"=="--streams" set "MATCHING_PROFILE=streams"

echo ==^> Starting Kafka
docker compose up -d
if errorlevel 1 (
  echo Could not start Kafka. Is Docker Desktop running?
  exit /b 1
)

if "%GEMINI_API_KEY%"=="" (
  echo ==^> GEMINI_API_KEY is not set, so the two AI buttons will be greyed out.
  echo     To use the AI agents: close the service windows, run  set GEMINI_API_KEY=your-key  and start again.
)

echo ==^> Starting services in separate windows
start "user-service" cmd /k mvn -pl user-service spring-boot:run
start "job-service" cmd /k mvn -pl job-service spring-boot:run
start "application-service" cmd /k mvn -pl application-service spring-boot:run
if "%MATCHING_PROFILE%"=="" (
  start "matching-service" cmd /k mvn -pl matching-service spring-boot:run
) else (
  echo ==^> matching-service will use Kafka Streams analytics
  start "matching-service" cmd /k "set SPRING_PROFILES_ACTIVE=%MATCHING_PROFILE%&& mvn -pl matching-service spring-boot:run"
)

echo ==^> Waiting for the services to start (first run downloads libraries)
timeout /t 60 /nobreak >nul
start http://localhost:8081
echo.
echo CampusGig is running: http://localhost:8081
echo Close the four service windows and run "docker compose down" to stop.
