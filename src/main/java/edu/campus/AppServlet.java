package edu.campus;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.sql.SQLException;

public class AppServlet extends HttpServlet {
    private Db db;
    private Path uploads;
    private final Logger log = Logger.getLogger(AppServlet.class.getName());

    private record Attempt(long time, int count) {
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    private static class Problem extends RuntimeException {
        final int status;

        Problem(int s, String m) {
            super(m);
            status = s;
        }
    }

    @Override
    public void init() throws ServletException {
        try {
            db = new Db();
            db.init();
            uploads = Path.of(Security.config("UPLOAD_DIR", "./uploads")).toAbsolutePath();
            Files.createDirectories(uploads);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void fail(int s, String m) {
        throw new Problem(s, m);
    }

    private long id(HttpServletRequest r, String name) {
        try {
            long v = Long.parseLong(r.getParameter(name));
            if (v < 1)
                throw new NumberFormatException();
            return v;
        } catch (Exception e) {
            throw new Problem(400, "Invalid identifier.");
        }
    }

    private String text(HttpServletRequest r, String name, int min, int max) {
        String v = Optional.ofNullable(r.getParameter(name)).orElse("").trim();
        if (v.length() < min || v.length() > max)
            fail(400, name + " must contain " + min + " to " + max + " characters.");
        return v;
    }

    private String email(HttpServletRequest r) {
        String v = text(r, "email", 3, 254).toLowerCase(Locale.ROOT);
        if (!v.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            fail(400, "Enter a valid email address.");
        return v;
    }

    private double number(HttpServletRequest r, String name, double min, double max) {
        try {
            double d = Double.parseDouble(r.getParameter(name));
            if (!Double.isFinite(d) || d < min || d > max)
                throw new NumberFormatException();
            return d;
        } catch (Exception e) {
            throw new Problem(400, name + " must be between " + min + " and " + max + ".");
        }
    }

    private String choice(HttpServletRequest r, String name, String... choices) {
        String v = text(r, name, 1, 40);
        if (!List.of(choices).contains(v))
            fail(400, "Invalid " + name + ".");
        return v;
    }

    private long uid(Map<String, Object> u) {
        return ((Number) u.get("id")).longValue();
    }

    private boolean is(Map<String, Object> u, String role) {
        return u != null && role.equals(u.get("role"));
    }

    private void require(Map<String, Object> u, String... roles) {
        if (u == null)
            fail(401, "Please sign in.");
        if (roles.length > 0 && !List.of(roles).contains(u.get("role")))
            fail(403, "You do not have permission to perform this action.");
    }

    private void view(HttpServletRequest r, HttpServletResponse s, String name) throws Exception {
        r.setAttribute("base", r.getContextPath() + "/app");
        r.getRequestDispatcher("/WEB-INF/views/" + name + ".jsp").forward(r, s);
    }

    private void redirect(HttpServletRequest r, HttpServletResponse s, String route) throws IOException {
        s.sendRedirect(r.getContextPath() + "/app/" + route);
    }

    private int page(HttpServletRequest r) {
        try {
            int p = Integer.parseInt(Optional.ofNullable(r.getParameter("page")).orElse("1"));
            if (p < 1 || p > 100000)
                throw new NumberFormatException();
            return p;
        } catch (Exception e) {
            throw new Problem(400, "Invalid page number.");
        }
    }

    private String scope(Map<String, Object> u) {
        return is(u, "student") ? " WHERE a.student_id=?"
                : is(u, "recruiter") ? " WHERE j.recruiter_id=?" : "";
    }

    private Object[] scopedArgs(Map<String, Object> u) {
        return is(u, "admin") ? new Object[] {} : new Object[] { uid(u) };
    }

    private void pagination(HttpServletRequest r, long total, int page) {
        r.setAttribute("total", total);
        r.setAttribute("page", page);
        r.setAttribute("pages", Math.max(1, (total + 7) / 8));
    }

    private void throttle(String ip) {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> now - e.getValue().time() > 900000);
        if (attempts.size() > 10000)
            fail(429, "Server busy. Try again later.");
        Attempt a = attempts.compute(ip,
                (k, v) -> v == null ? new Attempt(now, 1) : new Attempt(v.time(), v.count() + 1));
        if (a.count() > 30)
            fail(429, "Too many authentication attempts. Try again in 15 minutes.");
    }

    @Override
    protected void service(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        long start = System.nanoTime();
        r.setCharacterEncoding("UTF-8");
        s.setCharacterEncoding("UTF-8");
        s.setHeader("X-Content-Type-Options", "nosniff");
        s.setHeader("X-Frame-Options", "DENY");
        s.setHeader("Referrer-Policy", "same-origin");
        s.setHeader("Cache-Control", "no-store");
        s.setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'");
        boolean production = Security.config("APP_ENV", "development").equals("production");
        if (production)
            s.setHeader("Strict-Transport-Security", "max-age=31536000");
        try {
            if (production && !r.isSecure())
                fail(400, "HTTPS is required. Configure your TLS reverse proxy.");
            HttpSession session = r.getSession();
            if (session.getAttribute("csrf") == null)
                session.setAttribute("csrf", Security.token());
            Map<String, Object> user = null;
            if (session.getAttribute("uid") != null)
                user = db.one("SELECT id,name,email,role,branch,cgpa,skills FROM users WHERE id=?",
                        session.getAttribute("uid"));
            r.setAttribute("user", user);
            if (r.getMethod().equals("POST")
                    && Optional.ofNullable(r.getContentType()).orElse("").startsWith("multipart/form-data")) {
                try {
                    r.getParts();
                } catch (IllegalStateException | ServletException ex) {
                    fail(400, "Upload a valid PDF, maximum 5 MB.");
                }
            }
            if (r.getMethod().equals("POST")
                    && !Objects.equals(session.getAttribute("csrf"), r.getParameter("csrf")))
                fail(403, "Invalid CSRF token. Return to the page and try again.");
            String route = Optional.ofNullable(r.getPathInfo()).orElse("/dashboard");
            if (r.getMethod().equals("GET"))
                get(r, s, route, user);
            else if (r.getMethod().equals("POST"))
                post(r, s, route, user);
            else
                fail(405, "Method not allowed.");
        } catch (Problem e) {
            s.setStatus(e.status);
            r.setAttribute("message", e.getMessage());
            try {
                view(r, s, "error");
            } catch (Exception ex) {
                throw new ServletException(ex);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Request failed", e);
            s.setStatus(e instanceof SQLException && "23505".equals(((SQLException) e).getSQLState()) ? 409
                    : 500);
            r.setAttribute("message", s.getStatus() == 409 ? "This email or application already exists."
                    : "Something went wrong. Please try again.");
            try {
                view(r, s, "error");
            } catch (Exception ex) {
                throw new ServletException(ex);
            }
        } finally {
            log.info(r.getMethod() + " " + r.getRequestURI() + " status=" + s.getStatus() + " durationMs="
                    + (System.nanoTime() - start) / 1000000);
        }
    }

    private void get(HttpServletRequest r, HttpServletResponse s, String route, Map<String, Object> u)
            throws Exception {
        if (route.equals("/login") || route.equals("/register")) {
            r.setAttribute("register", route.equals("/register"));
            view(r, s, "auth");
            return;
        }
        if (u == null) {
            redirect(r, s, "login");
            return;
        }
        r.setAttribute("active", route);
        switch (route) {
        case "/":
        case "/dashboard": {
            r.setAttribute("jobsCount", db.count("SELECT COUNT(*) FROM jobs WHERE deadline>=CURRENT_DATE"));
            String sc = scope(u);
            r.setAttribute("applicationsCount", db.count(
                    "SELECT COUNT(*) FROM applications a JOIN jobs j ON j.id=a.job_id" + sc, scopedArgs(u)));
            r.setAttribute("selectedCount",
                    db.count(
                            "SELECT COUNT(*) FROM applications a JOIN jobs j ON j.id=a.job_id" + sc
                                    + (sc.isEmpty() ? " WHERE" : " AND") + " a.status='Selected'",
                            scopedArgs(u)));
            r.setAttribute("jobs", db.query(
                    "SELECT * FROM jobs WHERE deadline>=CURRENT_DATE ORDER BY created_at DESC,id DESC LIMIT 3"));
            view(r, s, "dashboard");
            break;
        }
        case "/jobs": {
            int p = page(r);
            String q = Optional.ofNullable(r.getParameter("q")).orElse("").trim();
            if (q.length() > 100)
                fail(400, "Search must be at most 100 characters.");
            String pattern = "%"
                    + q.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_")
                    + "%";
            String where = " WHERE (LOWER(title) LIKE ? ESCAPE '!' OR LOWER(company) LIKE ? ESCAPE '!')";
            r.setAttribute("jobs",
                    db.query(
                            "SELECT * FROM jobs" + where
                                    + " ORDER BY created_at DESC,id DESC LIMIT 8 OFFSET ?",
                            pattern, pattern, (p - 1) * 8));
            pagination(r, db.count("SELECT COUNT(*) FROM jobs" + where, pattern, pattern), p);
            r.setAttribute("q", q);
            view(r, s, "jobs");
            break;
        }
        case "/applications": {
            int p = page(r);
            String sc = scope(u);
            List<Object> args = new ArrayList<>(List.of(scopedArgs(u)));
            args.add((p - 1) * 8);
            r.setAttribute("applications", db.query(
                    "SELECT a.*,j.title,j.company,j.recruiter_id,u.name,u.email,u.resume FROM applications a JOIN jobs j ON j.id=a.job_id JOIN users u ON u.id=a.student_id"
                            + sc + " ORDER BY a.created_at DESC,a.id DESC LIMIT 8 OFFSET ?",
                    args.toArray()));
            pagination(r, db.count("SELECT COUNT(*) FROM applications a JOIN jobs j ON j.id=a.job_id" + sc,
                    scopedArgs(u)), p);
            view(r, s, "applications");
            break;
        }
        case "/profile":
            view(r, s, "profile");
            break;
        case "/new-job":
            require(u, "admin", "recruiter");
            view(r, s, "new-job");
            break;
        case "/users":
            require(u, "admin");
            int p = page(r);
            r.setAttribute("users", db.query(
                    "SELECT id,name,email,role FROM users ORDER BY id DESC LIMIT 8 OFFSET ?", (p - 1) * 8));
            pagination(r, db.count("SELECT COUNT(*) FROM users"), p);
            view(r, s, "users");
            break;
        case "/resume": {
            long student = r.getParameter("student") == null ? uid(u) : id(r, "student");
            boolean access = student == uid(u) || is(u, "admin") || (is(u, "recruiter") && db.count(
                    "SELECT COUNT(*) FROM applications a JOIN jobs j ON j.id=a.job_id WHERE a.student_id=? AND j.recruiter_id=?",
                    student, uid(u)) > 0);
            if (!access)
                fail(403, "This resume is private.");
            var row = db.one("SELECT resume FROM users WHERE id=?", student);
            if (row == null || row.get("resume") == null)
                fail(404, "No resume uploaded.");
            Path file = uploads.resolve(Path.of((String) row.get("resume")).getFileName());
            if (!Files.exists(file))
                fail(404, "File is unavailable.");
            s.setContentType("application/pdf");
            s.setHeader("Content-Disposition", "attachment; filename=resume.pdf");
            s.setContentLengthLong(Files.size(file));
            Files.copy(file, s.getOutputStream());
            break;
        }
        default:
            fail(404, "Page not found.");
        }
    }

    private void post(HttpServletRequest r, HttpServletResponse s, String route, Map<String, Object> u)
            throws Exception {
        switch (route) {
        case "/login": {
            throttle(r.getRemoteAddr());
            String email = email(r), password = text(r, "password", 1, 100);
            var found = db.one("SELECT * FROM users WHERE email=?", email);
            if (found == null || !Security.verify(password, (String) found.get("password")))
                fail(401, "Invalid email or password.");
            r.changeSessionId();
            r.getSession().setAttribute("uid", found.get("id"));
            r.getSession().setAttribute("csrf", Security.token());
            redirect(r, s, "dashboard");
            return;
        }
        case "/register": {
            throttle(r.getRemoteAddr());
            String name = text(r, "name", 2, 80), email = email(r), password = text(r, "password", 12, 100);
            long student = db.update("INSERT INTO users(name,email,password,role) VALUES(?,?,?,'student')",
                    name, email, Security.hash(password));
            r.changeSessionId();
            r.getSession().setAttribute("uid", student);
            r.getSession().setAttribute("csrf", Security.token());
            redirect(r, s, "profile");
            return;
        }
        }
        require(u);
        switch (route) {
        case "/logout":
            r.getSession().invalidate();
            redirect(r, s, "login");
            break;
        case "/profile":
            require(u, "student");
            db.update("UPDATE users SET name=?,branch=?,cgpa=?,skills=? WHERE id=?", text(r, "name", 2, 80),
                    text(r, "branch", 2, 80).toUpperCase(Locale.ROOT), number(r, "cgpa", 0, 10),
                    text(r, "skills", 0, 1000), uid(u));
            redirect(r, s, "profile?saved=1");
            break;
        case "/jobs": {
            require(u, "admin", "recruiter");
            LocalDate deadline;
            try {
                deadline = LocalDate.parse(r.getParameter("deadline"));
            } catch (Exception e) {
                throw new Problem(400, "Enter a valid deadline.");
            }
            if (deadline.isBefore(LocalDate.now()))
                fail(400, "Deadline cannot be in the past.");
            db.update(
                    "INSERT INTO jobs(title,company,location,kind,description,min_cgpa,branch,stipend,deadline,recruiter_id) VALUES(?,?,?,?,?,?,?,?,?,?)",
                    text(r, "title", 3, 120), text(r, "company", 2, 120), text(r, "location", 2, 120),
                    choice(r, "kind", "Job", "Internship"), text(r, "description", 20, 5000),
                    number(r, "min_cgpa", 0, 10), text(r, "branch", 2, 80).toUpperCase(Locale.ROOT),
                    text(r, "stipend", 1, 100), java.sql.Date.valueOf(deadline), uid(u));
            redirect(r, s, "jobs");
            break;
        }
        case "/apply": {
            require(u, "student");
            long job = id(r, "job");
            var j = db.one("SELECT * FROM jobs WHERE id=?", job);
            if (j == null)
                fail(404, "Job not found.");
            if (((java.sql.Date) j.get("deadline")).toLocalDate().isBefore(LocalDate.now()))
                fail(409, "Applications for this job are closed.");
            if (((Number) u.get("cgpa")).doubleValue() < ((Number) j.get("min_cgpa")).doubleValue()
                    || (!j.get("branch").equals("ANY") && !j.get("branch").equals(u.get("branch"))))
                fail(403, "Your profile does not meet the eligibility criteria.");
            var profile = db.one("SELECT resume FROM users WHERE id=?", uid(u));
            if (profile.get("resume") == null)
                fail(400, "Upload your resume before applying.");
            db.update("INSERT INTO applications(job_id,student_id) VALUES(?,?)", job, uid(u));
            redirect(r, s, "applications");
            break;
        }
        case "/status": {
            require(u, "admin", "recruiter");
            long application = id(r, "application");
            var a = db.one(
                    "SELECT a.id,j.recruiter_id FROM applications a JOIN jobs j ON j.id=a.job_id WHERE a.id=?",
                    application);
            if (a == null)
                fail(404, "Application not found.");
            if (!is(u, "admin") && ((Number) a.get("recruiter_id")).longValue() != uid(u))
                fail(403, "You can manage only applications for your own jobs.");
            db.update("UPDATE applications SET status=? WHERE id=?",
                    choice(r, "status", "Applied", "Under Review", "Shortlisted", "Selected", "Rejected"),
                    application);
            redirect(r, s, "applications");
            break;
        }
        case "/users": {
            require(u, "admin");
            String password = text(r, "password", 12, 100);
            db.update("INSERT INTO users(name,email,password,role) VALUES(?,?,?,?)", text(r, "name", 2, 80),
                    email(r), Security.hash(password), choice(r, "role", "recruiter", "student"));
            redirect(r, s, "users");
            break;
        }
        case "/resume": {
            require(u, "student");
            Part file;
            try {
                file = r.getPart("file");
            } catch (IllegalStateException e) {
                throw new Problem(400, "Resume must be 5 MB or smaller.");
            }
            if (file == null || file.getSize() < 5 || file.getSize() > 5 * 1024 * 1024
                    || !"application/pdf".equals(file.getContentType()) || file.getSubmittedFileName() == null
                    || !file.getSubmittedFileName().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                fail(400, "Upload a PDF resume, maximum 5 MB.");
            byte[] bytes;
            try (var in = file.getInputStream()) {
                bytes = in.readAllBytes();
            }
            if (!new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
                fail(400, "File content is not a PDF.");
            String name = UUID.randomUUID() + ".pdf";
            Path target = uploads.resolve(name);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            var old = db.one("SELECT resume FROM users WHERE id=?", uid(u));
            try {
                db.update("UPDATE users SET resume=? WHERE id=?", name, uid(u));
            } catch (Exception e) {
                Files.deleteIfExists(target);
                throw e;
            }
            if (old.get("resume") != null)
                Files.deleteIfExists(uploads.resolve(Path.of((String) old.get("resume")).getFileName()));
            redirect(r, s, "profile?saved=1");
            break;
        }
        default:
            fail(404, "Action not found.");
        }
    }
}
