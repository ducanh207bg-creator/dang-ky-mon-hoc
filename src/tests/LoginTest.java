import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

/** Chạy trong thư mục src: java -cp "out;lib/*" tests/LoginTest.java */
public class LoginTest {
    private static final String BASE = "http://127.0.0.1:8765";
    // Mật khẩu mẫu, xem data/setup.sql
    private static final String PASSWORD = "demo1234";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static int failures;

    public static void main(String[] args) throws Exception {
        Path database = Files.createTempDirectory("login-test").resolve("test.db");
        // Cơ sở dữ liệu cũ: tên cũ và một môn đã đăng ký.
        try (var db = DriverManager.getConnection("jdbc:sqlite:" + database); var sql = db.createStatement()) {
            sql.executeUpdate("CREATE TABLE students (id TEXT PRIMARY KEY, name TEXT NOT NULL, class_name TEXT NOT NULL, major TEXT NOT NULL, max_credits INTEGER NOT NULL)");
            sql.executeUpdate("CREATE TABLE courses (id TEXT PRIMARY KEY, name TEXT NOT NULL, credits INTEGER NOT NULL, lecturer TEXT NOT NULL, capacity INTEGER NOT NULL, day INTEGER NOT NULL, start TEXT NOT NULL, end TEXT NOT NULL)");
            sql.executeUpdate("CREATE TABLE registrations (student_id TEXT NOT NULL, course_id TEXT NOT NULL, PRIMARY KEY(student_id, course_id))");
            sql.executeUpdate("INSERT INTO students VALUES ('SV01','Tên Cũ','CNTT01','Công nghệ thông tin',6)");
            sql.executeUpdate("INSERT INTO registrations VALUES ('SV01','MA101')");
        }
        Class.forName("main.WebServer").getMethod("main", String[].class)
                .invoke(null, (Object) new String[] {database.toString(), "8765"});

        String[] names = {"Trần Văn Đức Anh", "Nguyễn Văn Long", "Nguyễn Tất Cương", "Nguyễn Quang Minh"};
        for (int i = 0; i < names.length; i++) {
            var r = login("SV0" + (i + 1), PASSWORD);
            check("member SV0" + (i + 1) + " can log in with their name",
                    r.statusCode() == 200 && r.body().contains(names[i]));
        }

        var wrong = login("SV01", "sai-mat-khau");
        var unknown = login("SV99", PASSWORD);
        check("wrong password is rejected", wrong.statusCode() == 401);
        check("unknown ID gets the same answer as wrong password",
                unknown.statusCode() == 401 && unknown.body().equals(wrong.body()));
        check("blank login is rejected", login("", "").statusCode() == 401);

        check("state needs login", send("GET", "/api/state?student=SV01", null, null).statusCode() == 401);
        check("text needs login", send("GET", "/api/text?student=SV01", null, null).statusCode() == 401);
        check("change needs login", send("POST", "/api/change",
                "{\"student\":\"SV01\",\"course\":\"IT101\",\"action\":\"register\"}", null).statusCode() == 401);
        check("made-up token is rejected", send("GET", "/api/state", null, "abc").statusCode() == 401);

        String one = token(login("SV01", PASSWORD).body());
        String two = token(login("SV02", PASSWORD).body());
        var state = send("GET", "/api/state?student=SV02", null, one).body();
        check("asking for another ID still shows own data",
                state.contains(names[0]) && !state.contains(names[1]));
        check("old registration is kept after name update", state.contains("\"credits\":3"));
        check("courses include a room", state.contains("\"room\":\"P.301\""));
        check("old 6-credit limit is raised to 18", state.contains("\"max_credits\":18"));
        check("there are 11 courses, each with a room",
                state.split("\"room\":\"P\\.", -1).length - 1 == 11);

        var change = send("POST", "/api/change",
                "{\"student\":\"SV02\",\"course\":\"EN101\",\"action\":\"register\"}", one);
        check("logged-in student can register", change.statusCode() == 200);
        check("registration goes to the logged-in student",
                send("GET", "/api/state", null, one).body().contains("\"credits\":5"));
        check("other student is unchanged",
                send("GET", "/api/state", null, two).body().contains("\"credits\":0"));

        check("logout succeeds", send("POST", "/api/logout", "", one).statusCode() == 200);
        check("token stops working after logout", send("GET", "/api/state", null, one).statusCode() == 401);
        check("other session stays logged in", send("GET", "/api/state", null, two).statusCode() == 200);

        System.out.println(failures == 0 ? "ALL PASSED" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static HttpResponse<String> login(String student, String password) throws Exception {
        return send("POST", "/api/login",
                "{\"student\":\"" + student + "\",\"password\":\"" + password + "\"}", null);
    }

    private static String token(String body) {
        var match = java.util.regex.Pattern.compile("\"token\":\"([^\"]+)\"").matcher(body);
        return match.find() ? match.group(1) : "none";
    }

    private static HttpResponse<String> send(String method, String path, String body, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(BASE + path)).method(method, body == null
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) failures++;
    }
}
