@echo off
REM Starts Kafka and all four CampusGig services, then opens the web UI.
cd /d "%~dp0"

where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven not found. Install it with:  winget install Apache.Maven
  echo or run the services from VS Code instead ^(see README^).
  exit /b 1
)

echo ==^> Starting Kafka
docker compose up -d
if errorlevel 1 (
  echo Could not start Kafka. Is Docker Desktop running?
  exit /b 1
)

echo ==^> Starting services in separate windows
start "user-service" cmd /k mvn -pl user-service spring-boot:run
start "job-service" cmd /k mvn -pl job-service spring-boot:run
start "application-service" cmd /k mvn -pl application-service spring-boot:run
start "matching-service" cmd /k mvn -pl matching-service spring-boot:run

echo ==^> Waiting for the services to start (first run downloads libraries)
timeout /t 60 /nobreak >nul
start http://localhost:8081
echo.
echo CampusGig is running: http://localhost:8081
echo Close the four service windows and run "docker compose down" to stop.
