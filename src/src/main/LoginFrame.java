package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Map;
import com.google.gson.Gson;
import javax.swing.*;

// Màn hình đăng nhập
class LoginFrame extends JFrame {
    private static final Color BANNER = new Color(0x102A4F);
    private static final Color BLUE = new Color(0x2866A9);
    private static final Color BOX = new Color(0xF3F3F4);
    private static final Color GRAY = new Color(0x4B5563);

    private final JTextField txtStudentId = LoginParts.textField("Ví dụ: SV01");
    private final JPasswordField txtPassword = LoginParts.passwordField("Nhập mật khẩu");

    LoginFrame() {
        super("Đăng ký môn học - Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);
        content.add(createBanner(), BorderLayout.NORTH);
        content.add(createForm(), BorderLayout.CENTER);
        setContentPane(content);

        // Phải khóa kích thước trước pack(), nếu không chữ ở mép phải có thể bị cắt
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }

    // Phần đầu màu xanh đậm: tên trường và tên hệ thống
    private JPanel createBanner() {
        JPanel school = new JPanel(new GridLayout(2, 1));
        school.setOpaque(false);
        school.add(label("PHENIKAA UNIVERSITY", Font.BOLD, 14, Color.WHITE));
        school.add(label("STUDENT COURSE REGISTRATION", Font.PLAIN, 11, Color.WHITE));

        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        brand.add(new JLabel(new LoginParts.CapIcon()), BorderLayout.WEST);
        brand.add(school, BorderLayout.CENTER);

        JPanel banner = new JPanel(new GridBagLayout());
        banner.setBackground(BANNER);
        banner.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        addRow(banner, brand, 14);
        addRow(banner, label("Hệ thống đăng ký môn học", Font.BOLD, 22, Color.WHITE), 6);
        addRow(banner, label("Đăng ký học phần trực tuyến – Học kỳ 1 năm học 2026–2027",
                Font.PLAIN, 13, Color.WHITE), 0);
        return banner;
    }

    // Phần dưới: khung xám chứa ô nhập và nút đăng nhập
    private JPanel createForm() {
        JButton btnLogin = LoginParts.button("ĐĂNG NHẬP", BLUE);
        btnLogin.addActionListener(e -> login());
        getRootPane().setDefaultButton(btnLogin);

        JPanel box = new LoginParts.RoundPanel(new GridBagLayout(), BOX, 18);
        box.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
        addRow(box, new JLabel(new LoginParts.UserIcon(), SwingConstants.CENTER), 4);
        addRow(box, centered("Chào mừng sinh viên", Font.BOLD, 15, Color.BLACK), 2);
        addRow(box, centered("Vui lòng đăng nhập để tiếp tục", Font.PLAIN, 11, GRAY), 14);
        addRow(box, label("Mã sinh viên", Font.BOLD, 12, GRAY), 5);
        addRow(box, txtStudentId, 12);
        addRow(box, label("Mật khẩu", Font.BOLD, 12, GRAY), 5);
        addRow(box, txtPassword, 18);
        addRow(box, btnLogin, 12);
        addRow(box, centered("Giao diện minh họa – đồ án môn học", Font.PLAIN, 11, GRAY), 0);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(label("Đăng nhập sinh viên", Font.BOLD, 14, Color.BLACK), BorderLayout.WEST);
        heading.add(LoginParts.badge("Student Portal", new Color(0xDCEEFF), new Color(0x1478D4)),
                BorderLayout.EAST);

        JPanel form = new JPanel(new BorderLayout(0, 12));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        form.add(heading, BorderLayout.NORTH);
        form.add(box, BorderLayout.CENTER);
        return form;
    }

    private static JLabel label(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private static JLabel centered(String text, int style, int size, Color color) {
        JLabel label = label(text, style, size, color);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    // Thêm một dòng vào cuối khung
    private static void addRow(JPanel panel, JComponent component, int spaceBelow) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, spaceBelow, 0);
        panel.add(component, c);
    }

    private void login() {
        try {
            String body = new Gson().toJson(Map.of(
                    "student", txtStudentId.getText().trim(),
                    "password", new String(txtPassword.getPassword())));
            SwingApp.Response response = SwingApp.request("/api/login", body, null);
            if (response.status == 200) {
                dispose();
                SwingApp.open(response.body);
                return;
            }
            JOptionPane.showMessageDialog(this, response.body);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không kết nối được máy chủ. Hãy chạy main.WebServer trước.");
        }
        txtPassword.setText("");
        txtPassword.requestFocusInWindow();
    }
}
