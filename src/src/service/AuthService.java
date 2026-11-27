package service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import repository.SqlRepository;

/** Đăng nhập và quản lý phiên. Phiên được giữ trong bộ nhớ, mất khi máy chủ khởi động lại. */
public class AuthService {
    private final SqlRepository repository;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthService(SqlRepository repository) {
        this.repository = repository;
    }

    /** Trả về mã phiên, hoặc null nếu sai mã sinh viên hay mật khẩu. */
    public String login(String studentId, String password) throws SQLException, RegistrationException {
        if (studentId == null || password == null) return null;
        String id = studentId.trim().toUpperCase(Locale.ROOT);
        try (Connection db = repository.open()) {
            String[] credential = repository.credential(db, id);
            if (credential == null || !matches(password, credential[0], credential[1])) return null;
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            sessions.put(token, new Session(id, repository.student(db, id).getName()));
            return token;
        }
    }

    public String studentId(String token) {
        Session session = token == null ? null : sessions.get(token);
        return session == null ? null : session.studentId;
    }

    public String name(String token) {
        return sessions.get(token).name;
    }

    public void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    private static boolean matches(String password, String salt, String hash) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), 120000, 256);
            byte[] actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(actual, Base64.getDecoder().decode(hash));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class Session {
        private final String studentId;
        private final String name;

        private Session(String studentId, String name) {
            this.studentId = studentId;
            this.name = name;
        }
    }
}
