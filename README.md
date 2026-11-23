# Launchpad — College Placement & Internship Portal

A runnable B.Tech Web Technology project built with Java 17+, Jakarta Servlets 6, JSP, JDBC, H2 and locally bundled Bootstrap 5. No Spring or Node backend is used in this project.

## Features

- Responsive student, recruiter and placement officer workspaces.
- Student registration; administrator-created recruiters; login, logout and one-hour sessions.
- PBKDF2-HMAC-SHA256 password hashing with individual salts and 210,000 iterations.
- CSRF tokens, session ID renewal after login, output escaping, prepared SQL and authentication throttling.
- Jobs/internships with company, location, stipend, branch, minimum CGPA and deadline.
- Student profile, private PDF resume upload/download and server-side eligibility checks.
- Application tracking and recruiter-owned application status updates.
- Search using AJAX and eight-record pagination; empty, loading and error states.
- Database foreign keys, unique emails, unique job/student applications and supporting indexes.
- Integration tests using real embedded Tomcat, JSP compilation and in-memory H2.
- Docker, persistent volumes, Caddy HTTPS configuration, request logging and GitHub CI.

## Quick start with Docker

Install Docker Desktop and start its Linux engine. Copy `.env.example` to `.env` and change `DB_PASSWORD`, `ADMIN_EMAIL` and `ADMIN_PASSWORD`. Passwords must be at least 12 characters. Use a long random database password. Optional `DEMO_PASSWORD` creates sample student/recruiter accounts and three jobs in development only.

```powershell
Copy-Item .env.example .env
# Edit .env before continuing.
docker compose up --build -d
```

Open http://localhost:8080. Admin credentials are the values YOU placed in `.env`.
Optional demo emails: `student@college.test`, `recruiter@college.test`; their password is YOUR `DEMO_PASSWORD`. Seeds never overwrite existing accounts or passwords. No fixed default credentials are embedded.

## Run without Docker

Install JDK 17 or newer and Maven 3.9+. From this project directory, set environment variables in the same terminal. Java does not automatically read `.env` outside Docker.

```powershell
$env:ADMIN_EMAIL='admin@college.test'
$env:ADMIN_PASSWORD='set-your-own-long-password'
$env:DB_PASSWORD='set-your-own-database-password'
# Optional for populated demo screens:
$env:DEMO_PASSWORD='set-your-own-demo-password'
mvn test-compile exec:java "-Dexec.mainClass=edu.campus.DevServer" "-Dexec.classpathScope=test"
```

Open http://localhost:8080. The embedded server is for development. Production deployment uses the WAR on Tomcat. Stop with Ctrl+C. Data persists under `data/`; resumes under `uploads/`.

For a separate Tomcat 10.1 installation:

```text
mvn clean verify
```

Copy `target/placement.war` to Tomcat's `webapps/`, set environment variables before launching Tomcat, and open `http://localhost:8080/placement/`. The context path is supported throughout the application. Tomcat 9 uses `javax.servlet` and is not compatible with this Jakarta application.

## Student workflow

1. Register a student account.
2. Fill in branch and CGPA under My profile, then upload a PDF resume.
3. Browse/search opportunities; apply for an eligible open role.
4. Track the result under My applications.

The admin uses People to create recruiter accounts. Recruiters post opportunities, view applications for their own jobs, download those applicants' current resumes and update statuses. Admins can view all applications. Status selection is intentionally reversible and is not a restricted state machine.

## Database design

`users(id, name, email UNIQUE, password, role, branch, cgpa, skills, resume)`

`jobs(id, ..., recruiter_id -> users.id, deadline, min_cgpa, branch)`

`applications(id, job_id -> jobs.id, student_id -> users.id, status, UNIQUE(job_id, student_id))`

Schema creation and idempotent demo setup: `src/main/java/edu/campus/Db.java`.
H2 is a persistent embedded SQL database, chosen for a self-contained college project. Run a single app instance; a multi-instance production deployment would need a shared database and session store. Recruiters see the student's current resume rather than an application-time snapshot.

## Testing

```text
mvn clean verify
```

Tests cover password hashing, login/logout, CSRF, public registration role enforcement, duplicate emails, roles, profile validation/escaping, pagination validation, job creation, eligibility, duplicate applications, file-size/content checks, private downloads, recruiter ownership and main JSP page rendering. Tests use isolated temporary files and an in-memory database; they do not touch your real data.

## HTTPS deployment

On a Docker-capable server, point a hostname's DNS to the server, allow ports 80/443, and add `DOMAIN=placements.your-domain.example` to `.env` using your actual hostname.

```text
docker compose -f compose.yaml -f compose.production.yaml up --build -d
docker compose logs -f app
```

Caddy obtains and renews TLS certificates when DNS and inbound access are correctly configured. Production rejects insecure application requests. Tomcat's RemoteIpValve recognizes the trusted internal proxy's forwarded HTTPS header; secure-request session cookies are then marked Secure. The app port binds only to loopback. Do not expose Tomcat directly or allow untrusted containers onto the proxy network. Caddy access logs go to stdout; Tomcat request logs also go to stdout and its log directory.

Deployment files are supplied; no public server, domain or TLS certificate is provisioned by this ZIP. Docker deployment must be validated in your environment. Back up the database and resume volumes consistently; never commit credentials or uploaded documents. PDF checks validate extension, MIME, size and signature; they are not antivirus scanning or full PDF semantic validation.

## Project layout and Git

`src/main/java/edu/campus/`: servlet/controller, database and password utilities.
`src/main/webapp/WEB-INF/views/`: JSP screens and shared components.
`src/main/webapp/assets/`: Bootstrap, CSS and JavaScript.
`src/test/java/edu/campus/`: integration tests and development launcher.
`deploy/`: Tomcat and HTTPS proxy configuration.

The `.gitignore` excludes credentials, generated files and personal data. A local Git repository is initialized during packaging, but `.git` is intentionally not shipped in the ZIP. Start your own repository after extracting:

```text
git init
git add .
git commit -m "Initial placement portal"
```

Bootstrap is MIT licensed; its license is included under assets. Other dependencies remain under their own licenses.

## Syllabus mapping

HTML forms/tables/images and CSS layout: JSP screens. JavaScript documents/functions/objects: menu, upload checks and AJAX search. Client-server and HTTP: servlet GET/POST. Servlets/JSP: controller lifecycle, directives, implicit request/session objects and page rendering. JDBC: prepared statements and SQL relationships. Cookies/HttpSession: authentication. Bootstrap: responsive grids, forms, buttons and tables.

Reference: https://tomcat.apache.org/tomcat-10.1-doc/ and https://getbootstrap.com/docs/5.3/

## Included build

`release/placement.war` is the verified prebuilt WAR. Deploy it to Tomcat 10.1 after configuring environment variables. Rebuild it with `mvn clean verify` after changing source. Read `docs/TEST_REPORT.md` for the actual verification scope.
