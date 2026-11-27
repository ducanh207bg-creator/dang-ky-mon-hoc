package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.google.gson.Gson;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.table.DefaultTableModel;

// Màn hình đăng ký môn học
public class SwingApp extends JFrame {
    private static final Color BANNER = new Color(0x102A4F);
    private static final Color BLUE = new Color(0x2866A9);
    private static final Color GREEN = new Color(0x137A43);
    private static final Color RED = new Color(0xB42323);
    private static final Color GRAY = new Color(0x6B7280);

    private final String token;
    private final JLabel lblGreeting = label(" ", Font.BOLD, 17, Color.BLACK);
    private final JLabel lblStudentId = label(" ", Font.PLAIN, 12, GRAY);
    private final JLabel lblCredits = label("–", Font.BOLD, 22, GREEN);
    private final JLabel lblChosen = label("–", Font.BOLD, 22, Color.BLACK);
    private final JLabel lblPercent = label("0%", Font.PLAIN, 12, Color.BLACK);
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JLabel lblMessage = label(" ", Font.PLAIN, 13, GREEN);

    private final JTextField txtSearch = new JTextField(24);
    private final JToggleButton btnAll = new JToggleButton("Tất cả", true);
    private final JToggleButton btnOpen = new JToggleButton("Còn chỗ");
    private final JToggleButton btnMine = new JToggleButton("Đã chọn");
    private final JLabel lblCount = label(" ", Font.PLAIN, 12, GRAY);

    private final JPanel myCourses = new JPanel();
    private final JLabel lblMyCount = label("0 môn", Font.BOLD, 12, GREEN);
    private final JLabel lblTotal = label("–", Font.BOLD, 13, Color.BLACK);

    private final DefaultTableModel model = new DefaultTableModel(new String[] {
            "MÔN HỌC", "TÍN CHỈ", "GIẢNG VIÊN", "LỊCH HỌC", "PHÒNG", "SĨ SỐ", "TRẠNG THÁI", "THAO TÁC"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    private State state;
    private final List<Course> shown = new ArrayList<>(); // các môn đang hiện trong bảng

    private SwingApp(String token) {
        super("Đăng ký môn học");
        this.token = token;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1260, 760);
        setMinimumSize(new Dimension(1100, 640));
        setLocationRelativeTo(null);

        JPanel top = new JPanel(new BorderLayout());
        top.add(createBanner(), BorderLayout.NORTH);
        top.add(createSummary(), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));
        body.add(createCourseList(), BorderLayout.CENTER);
        body.add(createMyCourses(), BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.add(top, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);
        setContentPane(content);
    }

    // Thanh xanh đậm trên cùng
    private JPanel createBanner() {
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(label("Hệ thống đăng ký học phần", Font.BOLD, 20, Color.WHITE));
        titles.add(label("PHENIKAA STUDENT PORTAL  ·  Học kỳ 1 · Năm học 2026–2027",
                Font.PLAIN, 12, Color.WHITE));

        JPanel badge = new JPanel(new GridBagLayout());
        badge.setOpaque(false);
        badge.add(LoginParts.badge("Đã đăng nhập", new Color(0x0C4040), new Color(0x2FD375)));

        JPanel banner = new JPanel(new BorderLayout(12, 0));
        banner.setBackground(BANNER);
        banner.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        banner.add(new JLabel(new LoginParts.CapIcon()), BorderLayout.WEST);
        banner.add(titles, BorderLayout.CENTER);
        banner.add(badge, BorderLayout.EAST);
        return banner;
    }

    // Lời chào, số tín chỉ, tiến độ và dòng thông báo
    private JPanel createSummary() {
        JPanel student = new JPanel(new GridLayout(2, 1));
        student.add(lblGreeting);
        student.add(lblStudentId);

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.addActionListener(e -> logout());
        JPanel logoutHolder = new JPanel(new GridBagLayout());
        logoutHolder.add(btnLogout);

        JPanel greeting = new JPanel(new BorderLayout());
        greeting.add(student, BorderLayout.CENTER);
        greeting.add(logoutHolder, BorderLayout.EAST);

        JPanel stats = new JPanel(new GridLayout(1, 2, 12, 0));
        stats.add(stat("Tín chỉ đã đăng ký", lblCredits));
        stats.add(stat("Số môn đã chọn", lblChosen));

        // Vẽ thanh liền, không chia đoạn
        UIManager.put("ProgressBar.cellSpacing", 0);
        UIManager.put("ProgressBar.cellLength", 1);
        progress.setUI(new BasicProgressBarUI());
        progress.setForeground(BLUE);
        progress.setBackground(new Color(0xE2E5EA));
        progress.setBorderPainted(false);
        progress.setPreferredSize(new Dimension(100, 8));

        JPanel progressTitle = new JPanel(new BorderLayout());
        progressTitle.add(label("Tiến độ tín chỉ", Font.BOLD, 12, Color.BLACK), BorderLayout.WEST);
        progressTitle.add(lblPercent, BorderLayout.EAST);

        JPanel summary = new JPanel();
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));
        for (JComponent row : new JComponent[] {greeting, stats, progressTitle, progress, lblMessage}) {
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            summary.add(row);
            summary.add(Box.createVerticalStrut(8));
        }
        return summary;
    }

    private static JPanel stat(String title, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label(title, Font.PLAIN, 12, GRAY), BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    // Ô tìm kiếm, nút lọc và bảng môn học
    private JPanel createCourseList() {
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchBar.add(new JLabel("Tìm theo mã hoặc tên:  "));
        searchBar.add(txtSearch);
        searchBar.add(Box.createHorizontalStrut(16));
        ButtonGroup filters = new ButtonGroup();
        for (JToggleButton button : new JToggleButton[] {btnAll, btnOpen, btnMine}) {
            filters.add(button);
            button.addActionListener(e -> showCourses());
            styleFilter(button);
            searchBar.add(button);
        }

        JPanel heading = new JPanel(new GridLayout(3, 1, 0, 4));
        heading.add(label("Danh sách môn học mở", Font.BOLD, 14, Color.BLACK));
        heading.add(searchBar);
        heading.add(lblCount);

        // Gõ tới đâu lọc tới đó
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                showCourses();
            }

            public void removeUpdate(DocumentEvent e) {
                showCourses();
            }

            public void changedUpdate(DocumentEvent e) {
                showCourses();
            }
        });

        setUpTable();
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // Bảng dạng danh sách: mỗi môn một hàng, nút thao tác nằm ngay trên hàng
    private void setUpTable() {
        table.setRowHeight(54);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer((t, value, selected, focus, row, column) -> {
            JLabel header = label(String.valueOf(value), Font.BOLD, 11, BANNER);
            header.setOpaque(true);
            header.setBackground(new Color(0xE8F2FD));
            header.setBorder(BorderFactory.createEmptyBorder(11, 10, 11, 4));
            return header;
        });

        CourseCell cell = new CourseCell();
        int[] widths = {200, 55, 105, 100, 60, 85, 105, 118};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setCellRenderer(cell);
        }
        ToolTipManager.sharedInstance().registerComponent(table);

        // Bấm vào nút ở cột thao tác
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && isOnButton(e.getPoint())) {
                    useButton(table.rowAtPoint(e.getPoint()));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                table.setCursor(Cursor.getPredefinedCursor(
                        isOnButton(e.getPoint()) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        table.addMouseListener(mouse);
        table.addMouseMotionListener(mouse);

        // Dùng bàn phím: chọn một hàng rồi nhấn Enter
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "useButton");
        table.getActionMap().put("useButton", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useButton(table.getSelectedRow());
            }
        });
    }

    private boolean isOnButton(Point point) {
        int row = table.rowAtPoint(point);
        int column = table.columnAtPoint(point);
        if (row < 0 || column != CourseCell.ACTION) return false;
        Rectangle cell = table.getCellRect(row, column, false);
        Rectangle button = CourseCell.buttonBounds(cell.width, cell.height);
        button.translate(cell.x, cell.y);
        return button.contains(point);
    }

    // Đăng ký hoặc hủy môn ở hàng này, môn hết chỗ thì không làm gì
    private void useButton(int row) {
        if (row < 0 || row >= shown.size()) return;
        Course course = shown.get(row);
        if (course.registered) {
            change(course.id, "drop");
        } else if (course.enrolled < course.capacity) {
            change(course.id, "register");
        }
    }
    // Cột bên phải: các môn đã đăng ký và tổng số tín chỉ
    private JPanel createMyCourses() {
        JPanel heading = new JPanel(new BorderLayout());
        heading.add(label("Môn học của tôi", Font.BOLD, 14, Color.BLACK), BorderLayout.WEST);
        heading.add(lblMyCount, BorderLayout.EAST);

        myCourses.setLayout(new BoxLayout(myCourses, BoxLayout.Y_AXIS));
        JPanel listHolder = new JPanel(new BorderLayout());
        listHolder.add(myCourses, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(listHolder,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel total = new JPanel(new BorderLayout());
        total.add(label("Tổng số tín chỉ", Font.BOLD, 13, Color.BLACK), BorderLayout.CENTER);
        total.add(lblTotal, BorderLayout.EAST);

        JButton btnRefresh = new JButton("Làm mới danh sách đăng ký");
        btnRefresh.addActionListener(e -> {
            if (loadCourses()) showMessage("Đã làm mới danh sách.", true);
        });

        JPanel bottom = new JPanel(new GridLayout(2, 1, 0, 8));
        bottom.add(total);
        bottom.add(btnRefresh);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setPreferredSize(new Dimension(270, 100));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // Nút lọc đang chọn có nền xanh đậm, các nút còn lại nền trắng
    private static void styleFilter(JToggleButton button) {
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        button.addItemListener(e -> paintFilter(button));
        paintFilter(button);
    }

    private static void paintFilter(JToggleButton button) {
        button.setBackground(button.isSelected() ? BANNER : Color.WHITE);
        button.setForeground(button.isSelected() ? Color.WHITE : Color.BLACK);
    }

    private static JLabel label(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private void showMessage(String text, boolean success) {
        lblMessage.setForeground(success ? GREEN : RED);
        lblMessage.setText(text);
    }

    static void open(String token) {
        SwingApp app = new SwingApp(token);
        app.setVisible(true);
        if (app.loadCourses()) {
            app.showMessage("Đăng nhập thành công. Chào mừng " + app.state.student.name + "!", true);
        }
    }

    // Tải dữ liệu mới từ máy chủ, trả về true nếu thành công
    private boolean loadCourses() {
        try {
            Response response = request("/api/state", null, token);
            if (response.status == 401) {
                backToLogin(response.body);
                return false;
            }
            if (response.status != 200) {
                showMessage(response.body, false);
                return false;
            }
            state = new Gson().fromJson(response.body, State.class);
            showSummary();
            showCourses();
            return true;
        } catch (IOException e) {
            showMessage("Không kết nối được máy chủ. Hãy chạy main.WebServer trước.", false);
            return false;
        }
    }

    private void showSummary() {
        int max = state.student.maxCredits();
        int percent = Math.min(100, Math.round(state.credits * 100f / max));
        lblGreeting.setText("Xin chào, " + state.student.name + "!");
        lblStudentId.setText("Mã sinh viên: " + state.student.id);
        lblCredits.setText(state.credits + "/" + max);
        lblTotal.setText(state.credits + "/" + max);
        lblPercent.setText(percent + "%");
        progress.setValue(percent);

        myCourses.removeAll();
        int count = 0;
        for (Course course : state.courses) {
            if (course.registered) {
                myCourses.add(myCourseRow(course));
                count++;
            }
        }
        if (count == 0) {
            myCourses.add(label("  Bạn chưa đăng ký môn nào.", Font.PLAIN, 12, GRAY));
        }
        lblChosen.setText(String.valueOf(count));
        lblMyCount.setText(count + " môn");
        myCourses.revalidate();
        myCourses.repaint();
    }

    private JPanel myCourseRow(Course course) {
        JPanel info = new JPanel(new GridLayout(2, 1));
        info.add(label(course.id + " – " + course.name, Font.BOLD, 12, Color.BLACK));
        info.add(label(course.credits + " tín chỉ · " + course.schedule, Font.PLAIN, 11, GRAY));

        JButton remove = new JButton("×");
        remove.setForeground(RED);
        remove.setToolTipText("Hủy môn " + course.id);
        remove.addActionListener(e -> change(course.id, "drop"));
        JPanel removeHolder = new JPanel(new GridBagLayout());
        removeHolder.add(remove);

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 6));
        row.add(info, BorderLayout.CENTER);
        row.add(removeHolder, BorderLayout.EAST);
        return row;
    }

    // Hiện các môn theo ô tìm kiếm và nút lọc đang chọn
    private void showCourses() {
        if (state == null) return;
        Locale vietnamese = Locale.forLanguageTag("vi");
        String text = txtSearch.getText().trim().toLowerCase(vietnamese);
        Course previous = selectedCourse();

        shown.clear();
        model.setRowCount(0);
        for (Course c : state.courses) {
            boolean full = c.enrolled >= c.capacity;
            if (btnOpen.isSelected() && full) continue;
            if (btnMine.isSelected() && !c.registered) continue;
            if (!(c.id + " " + c.name).toLowerCase(vietnamese).contains(text)) continue;

            shown.add(c);
            // Mọi cột cùng nhận cả môn học, CourseCell tự chọn phần cần vẽ
            model.addRow(new Object[] {c, c, c, c, c, c, c, c});
            // Giữ lại dòng đang chọn sau khi tải lại
            if (previous != null && c.id.equals(previous.id)) {
                table.setRowSelectionInterval(shown.size() - 1, shown.size() - 1);
            }
        }
        lblCount.setText("Hiển thị " + shown.size() + " môn học");
    }

    private Course selectedCourse() {
        int row = table.getSelectedRow();
        return row >= 0 && row < shown.size() ? shown.get(row) : null;
    }

    private void change(String courseId, String action) {
        try {
            String body = new Gson().toJson(Map.of("course", courseId, "action", action));
            Response response = request("/api/change", body, token);
            if (response.status == 401) {
                backToLogin(response.body);
                return;
            }
            boolean success = response.status == 200;
            // Tải lại cả khi lỗi, vì sĩ số có thể đã thay đổi
            if (loadCourses()) {
                showMessage(response.body, success);
            }
        } catch (IOException e) {
            showMessage("Không kết nối được máy chủ.", false);
        }
    }

    private void logout() {
        try {
            request("/api/logout", "", token);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không kết nối được máy chủ.");
        }
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void backToLogin(String message) {
        JOptionPane.showMessageDialog(this, message);
        dispose();
        new LoginFrame().setVisible(true);
    }

    // Gửi yêu cầu tới máy chủ, body == null thì dùng GET
    static Response request(String path, String body, String token) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create("http://127.0.0.1:8000" + path)
                .toURL().openConnection();
        connection.setRequestProperty("Accept", "text/plain");
        if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        if (body != null) {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        int status = connection.getResponseCode();
        try (InputStream in = status < 400 ? connection.getInputStream() : connection.getErrorStream()) {
            return new Response(status, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } finally {
            connection.disconnect();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Không đổi được thì dùng giao diện mặc định
            }
            // Dùng Segoe UI để dấu tiếng Việt hiển thị rõ
            Font font = new Font("Segoe UI", Font.PLAIN, 13);
            for (String name : new String[] {"Label", "Button", "ToggleButton", "CheckBox", "TextField",
                    "PasswordField", "Table", "TableHeader"}) {
                UIManager.put(name + ".font", new FontUIResource(font));
            }
            UIManager.put("OptionPane.messageFont", new FontUIResource(font));
            new LoginFrame().setVisible(true);
        });
    }

    static class Response {
        final int status;
        final String body;

        private Response(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    // Các lớp dưới đây khớp với JSON của /api/state
    private static class State {
        StudentInfo student;
        int credits;
        List<Course> courses;
    }

    private static class StudentInfo {
        String id;
        String name;
        private int max_credits;

        int maxCredits() {
            return max_credits;
        }
    }

    static class Course {
        String id;
        String name;
        int credits;
        String lecturer;
        String schedule;
        String room;
        int enrolled;
        int capacity;
        boolean registered;
    }
}
