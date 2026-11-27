package repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.*;
import service.*;

public class SqlRepository {
    private final String url;

    public SqlRepository(Path database) throws Exception {
        url = "jdbc:sqlite:" + database;
        try (Connection db = open(); Statement sql = db.createStatement()) {
            for (String statement : Files.readString(Path.of("data/setup.sql")).split(";")) {
                if (!statement.isBlank()) sql.executeUpdate(statement);
            }
        }
    }

    public Connection open() throws SQLException {
        Connection db = DriverManager.getConnection(url);
        try (Statement sql = db.createStatement()) {
            sql.execute("PRAGMA foreign_keys = ON");
            sql.execute("PRAGMA busy_timeout = 5000");
            return db;
        } catch (SQLException e) {
            db.close();
            throw e;
        }
    }

    public Student student(Connection db, String id) throws SQLException, RegistrationException {
        try (PreparedStatement sql = db.prepareStatement("SELECT * FROM students WHERE id=?")) {
            sql.setString(1, id);
            try (ResultSet row = sql.executeQuery()) {
                if (!row.next()) throw new RegistrationException("Không tìm thấy sinh viên.");
                return new Student(id, row.getString("name"), row.getString("class_name"),
                        row.getString("major"), row.getInt("max_credits"));
            }
        }
    }

    /** Trả về {salt, hash} dạng Base64, hoặc null nếu không có tài khoản. */
    public String[] credential(Connection db, String id) throws SQLException {
        try (PreparedStatement sql = db.prepareStatement("SELECT salt, hash FROM credentials WHERE student_id=?")) {
            sql.setString(1, id);
            try (ResultSet row = sql.executeQuery()) {
                return row.next() ? new String[] {row.getString(1), row.getString(2)} : null;
            }
        }
    }

    /** Trả về phòng học theo mã môn. */
    public Map<String, String> rooms(Connection db) throws SQLException {
        Map<String, String> rooms = new HashMap<>();
        try (Statement sql = db.createStatement();
             ResultSet row = sql.executeQuery("SELECT course_id, room FROM course_rooms")) {
            while (row.next()) rooms.put(row.getString(1), row.getString(2));
        }
        return rooms;
    }

    public RegistrationService load(Connection db) throws SQLException, RegistrationException {
        List<Course> catalog = new ArrayList<>();
        try (Statement sql = db.createStatement();
             ResultSet row = sql.executeQuery("SELECT * FROM courses ORDER BY id")) {
            while (row.next()) {
                Schedule schedule = new Schedule(DayOfWeek.of(Math.floorMod(row.getInt("day") - 2, 7) + 1),
                        LocalTime.parse(row.getString("start")), LocalTime.parse(row.getString("end")));
                catalog.add(new Course(row.getString("id"), row.getString("name"), row.getInt("credits"),
                        row.getString("lecturer"), row.getInt("capacity"), schedule));
            }
        }
        RegistrationService service = new RegistrationService(catalog);
        try (Statement sql = db.createStatement();
             ResultSet row = sql.executeQuery("SELECT student_id, course_id FROM registrations")) {
            while (row.next()) service.restoreRegistration(row.getString(1), row.getString(2));
        }
        return service;
    }

    public String change(String studentId, String courseId, String action) throws SQLException, RegistrationException {
        try (Connection db = open(); Statement transaction = db.createStatement()) {
            // Giữ khóa ghi từ trước bước kiểm tra để hai giao diện không lấy cùng một chỗ.
            transaction.execute("BEGIN IMMEDIATE");
            try {
                Student student = student(db, studentId);
                RegistrationService service = load(db);
                String command;
                String message;
                if ("register".equals(action)) {
                    service.register(student, courseId);
                    command = "INSERT INTO registrations(student_id, course_id) VALUES (?,?)";
                    message = "Đăng ký thành công.";
                } else if ("drop".equals(action)) {
                    service.drop(student, courseId);
                    command = "DELETE FROM registrations WHERE student_id=? AND course_id=?";
                    message = "Hủy đăng ký thành công.";
                } else {
                    throw new RegistrationException("Thao tác không hợp lệ.");
                }
                try (PreparedStatement sql = db.prepareStatement(command)) {
                    sql.setString(1, studentId);
                    sql.setString(2, courseId);
                    sql.executeUpdate();
                }
                transaction.execute("COMMIT");
                return message;
            } catch (SQLException | RegistrationException | RuntimeException e) {
                transaction.execute("ROLLBACK");
                throw e;
            }
        }
    }
}
