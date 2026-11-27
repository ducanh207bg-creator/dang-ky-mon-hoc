package main;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import model.*;
import repository.SqlRepository;
import service.*;

public class WebServer {
    private static final Gson JSON = new Gson();
    private static final String LOGIN_REQUIRED = "Vui lòng đăng nhập.";
    private final SqlRepository repository;
    private final AuthService auth;

    private WebServer(SqlRepository repository) {
        this.repository = repository;
        this.auth = new AuthService(repository);
    }

    private void handle(HttpExchange request) throws IOException {
        String path = request.getRequestURI().getPath();
        String method = request.getRequestMethod();
        try {
            if ("/".equals(path) && "GET".equals(method)) {
                send(request, 200, "text/html", Files.readString(Path.of("web/index.html")));
            } else if ("/api/login".equals(path) && "POST".equals(method)) {
                Login login = readBody(request, Login.class);
                String token = auth.login(login.student, login.password);
                if (token == null) {
                    error(request, path, 401, "Sai mã sinh viên hoặc mật khẩu.");
                } else if (wantsText(request)) {
                    send(request, 200, "text/plain", token);
                } else {
                    json(request, 200, Map.of("token", token, "name", auth.name(token)));
                }
            } else if ("/api/logout".equals(path) && "POST".equals(method)) {
                auth.logout(token(request));
                json(request, 200, Map.of("message", "Đã đăng xuất."));
            } else if ("/api/change".equals(path) && "POST".equals(method)) {
                String student = auth.studentId(token(request));
                if (student == null) {
                    error(request, path, 401, LOGIN_REQUIRED);
                    return;
                }
                Change change = readBody(request, Change.class);
                String message = repository.change(student, code(change.course), change.action);
                if (wantsText(request)) {
                    send(request, 200, "text/plain", message);
                } else {
                    json(request, 200, Map.of("message", message));
                }
            } else if (("/api/state".equals(path) || "/api/text".equals(path)) && "GET".equals(method)) {
                String student = auth.studentId(token(request));
                if (student == null) {
                    error(request, path, 401, LOGIN_REQUIRED);
                    return;
                }
                showCourses(request, student, "/api/text".equals(path));
            } else {
                json(request, 404, Map.of("error", "Không tìm thấy trang."));
            }
        } catch (RegistrationException | IllegalArgumentException | JsonParseException e) {
            error(request, path, 400, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            json(request, 500, Map.of("error", "Lỗi cơ sở dữ liệu."));
        } finally {
            request.close();
        }
    }

    private static <T> T readBody(HttpExchange request, Class<T> type) throws IOException {
        byte[] body = request.getRequestBody().readNBytes(4097);
        if (body.length > 4096) throw new IllegalArgumentException("Dữ liệu quá lớn.");
        T data = JSON.fromJson(new String(body, StandardCharsets.UTF_8), type);
        if (data == null) throw new IllegalArgumentException("Thiếu dữ liệu.");
        return data;
    }

    private static String token(HttpExchange request) {
        String header = request.getRequestHeaders().getFirst("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }

    private static boolean wantsText(HttpExchange request) {
        return "text/plain".equals(request.getRequestHeaders().getFirst("Accept"));
    }

    private static void error(HttpExchange request, String path, int status, String message) throws IOException {
        if ("/api/text".equals(path) || wantsText(request)) {
            send(request, status, "text/plain", message);
        } else {
            json(request, status, Map.of("error", message));
        }
    }

    private void showCourses(HttpExchange request, String studentId, boolean plain) throws SQLException, RegistrationException, IOException {
        Map<String, String> query = new HashMap<>();
        String raw = request.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String part : raw.split("&")) {
                String[] fields = part.split("=", 2);
                if (fields.length == 2) query.put(fields[0], URLDecoder.decode(fields[1], StandardCharsets.UTF_8));
            }
        }
        try (Connection db = repository.open()) {
            db.setAutoCommit(false);
            Student student = repository.student(db, studentId);
            RegistrationService service = repository.load(db);
            Collection<Course> registered = service.registeredCourses(student);
            int credits = service.totalCredits(student);
            Map<String, String> rooms = repository.rooms(db);
            List<Map<String, Object>> courses = new ArrayList<>();
            StringBuilder display = new StringBuilder(student.getName())
                    .append(" | Tín chỉ: ").append(credits).append("/").append(student.getMaxCredits()).append("\n\n");
            for (Course c : service.search(query.getOrDefault("q", ""))) {
                boolean selected = registered.contains(c);
                if ("true".equals(query.get("registered")) && !selected) continue;
                int enrolled = service.enrolledCount(c);
                courses.add(Map.of("id", c.getId(), "name", c.getName(), "credits", c.getCredits(),
                        "lecturer", c.getLecturer(), "capacity", c.getCapacity(), "enrolled", enrolled,
                        "schedule", c.getSchedule().toString(), "registered", selected,
                        "room", rooms.getOrDefault(c.getId(), "Chưa xếp")));
                display.append(String.format("%s | %s | %d TC | %s | %s | %d/%d%s%n", c.getId(), c.getName(),
                        c.getCredits(), c.getLecturer(), c.getSchedule(), enrolled, c.getCapacity(),
                        selected ? " | Đã đăng ký" : ""));
            }
            db.commit();
            if (plain) send(request, 200, "text/plain", display.toString());
            else json(request, 200, Map.of("student", Map.of("id", student.getId(), "name", student.getName(), "max_credits",
                    student.getMaxCredits()), "credits", credits, "courses", courses));
        }
    }

    private static String code(String input) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Vui lòng nhập mã.");
        return input.trim().toUpperCase(Locale.ROOT);
    }

    private static void json(HttpExchange request, int status, Object data) throws IOException {
        send(request, status, "application/json", JSON.toJson(data));
    }

    private static void send(HttpExchange request, int status, String type, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        request.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        request.sendResponseHeaders(status, bytes.length);
        request.getResponseBody().write(bytes);
    }

    private static class Login {
        String student;
        String password;
    }

    private static class Change {
        String course;
        String action;
    }

    public static void main(String[] args) throws Exception {
        Path database = Path.of(args.length > 0 ? args[0] : "data/registration.db");
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8000;
        WebServer app = new WebServer(new SqlRepository(database));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", app::handle);
        server.start();
        System.out.println("Mở http://127.0.0.1:" + server.getAddress().getPort());
    }
}
