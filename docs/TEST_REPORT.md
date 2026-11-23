# Verification report

Verified on 21 November 2026 in the build workspace.

- Maven verify: WAR packaging and 6 JUnit integration tests: PASS.
- Failed tests: 0; skipped tests: 0.
- Tomcat 10.1.61, Java 27 executing Java-17-targeted bytecode, H2 2.3.232 used for local verification.
- Login and student dashboard opened in a browser.
- Main JSP views compiled and rendered under the embedded Tomcat test suite. AJAX search is provided with server-rendered fallback.
- Responsive CSS reviewed in desktop and mobile previews.
- Password verification, roles, CSRF, eligibility, duplicate constraints, upload limits, resume access and recruiter ownership tested.


## Verification limits

Docker CLI is installed, but the Docker engine was not running on the build machine. Compose/container deployment was not executed. CI on Linux/JDK 17 or Node 22 is configured but was not run on a remote CI service. No public host/domain was configured and no real HTTPS certificate was issued. These are local development projects with deployment configuration, not hosted production services.

Accessibility and responsive layouts were manually sampled, not exhaustively audited across all browsers. Upload validation checks size, MIME, extension and PDF signature; it does not scan for malware.
