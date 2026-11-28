package main;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

// Vẽ lịch tuần: cột là thứ, trục dọc là giờ, mỗi môn là một khối đặt đúng theo giờ học
class CalendarGrid extends JComponent {
    private static final String[] DAYS = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};
    private static final Color[] COLORS = {new Color(0x223671), new Color(0xEA4C00), new Color(0x3B5296),
            new Color(0x5878D8), new Color(0x0F766E), new Color(0xBE185D), new Color(0x6D28D9),
            new Color(0x15803D)};
    private static final Color LINE = new Color(0xE5E7EB);
    private static final Color TEXT = new Color(0x1F2937);

    private static final int FIRST_HOUR = 6;
    private static final int LAST_HOUR = 19;
    private static final double PIXELS_PER_MINUTE = 1.1;
    private static final int HEADER = 36;
    private static final int HOUR_COLUMN = 52;

    private List<SwingApp.Course> courses = new ArrayList<>();

    CalendarGrid() {
        int hours = LAST_HOUR - FIRST_HOUR;
        setPreferredSize(new Dimension(1000, HEADER + (int) (hours * 60 * PIXELS_PER_MINUTE) + 8));
    }

    void setCourses(List<SwingApp.Course> registered) {
        courses = new ArrayList<>(registered);
        repaint();
    }

    // Vị trí theo chiều dọc của một mốc giờ dạng "09:30"
    private static int position(String time) {
        int minutes = (Integer.parseInt(time.substring(0, 2)) - FIRST_HOUR) * 60
                + Integer.parseInt(time.substring(3, 5));
        return HEADER + (int) Math.round(minutes * PIXELS_PER_MINUTE);
    }

    // Mỗi ca gồm 3 tiết, tra theo giờ bắt đầu
    private static String period(String start) {
        switch (start) {
            case "06:45": return " (Tiết 1-3)";
            case "09:30": return " (Tiết 4-6)";
            case "13:00": return " (Tiết 7-9)";
            case "15:45": return " (Tiết 10-12)";
            default: return "";
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        double dayWidth = (getWidth() - HOUR_COLUMN) / (double) DAYS.length;

        // Tên các thứ và đường kẻ
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(TEXT);
        g2.drawString("Giờ VN", 6, 22);
        for (int day = 0; day < DAYS.length; day++) {
            int x = HOUR_COLUMN + (int) (day * dayWidth);
            g2.setColor(LINE);
            g2.drawLine(x, 0, x, getHeight());
            g2.setColor(TEXT);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString(DAYS[day], x + 8, 22);
        }
        g2.setColor(new Color(0x374151));
        g2.drawLine(0, HEADER, getWidth(), HEADER);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        for (int hour = FIRST_HOUR + 1; hour < LAST_HOUR; hour++) {
            int y = position(String.format("%02d:00", hour));
            g2.setColor(LINE);
            g2.drawLine(HOUR_COLUMN, y, getWidth(), y);
            g2.setColor(TEXT);
            g2.drawString(hour + ":00", 14, y + 4);
        }

        for (int i = 0; i < courses.size(); i++) {
            paintCourse(g2, courses.get(i), COLORS[i % COLORS.length], dayWidth);
        }
        g2.dispose();
    }

    private void paintCourse(Graphics2D g2, SwingApp.Course course, Color color, double dayWidth) {
        // Lịch có dạng "Thứ 2 09:30–12:10"
        int split = course.schedule.lastIndexOf(' ');
        int day = java.util.Arrays.asList(DAYS).indexOf(course.schedule.substring(0, split));
        if (day < 0) return;
        String time = course.schedule.substring(split + 1);
        String start = time.substring(0, 5);
        String end = time.substring(time.length() - 5);

        int x = HOUR_COLUMN + (int) (day * dayWidth) + 3;
        int width = (int) dayWidth - 5;
        int y = position(start);
        int height = position(end) - y;

        Graphics2D box = (Graphics2D) g2.create();
        Shape shape = new RoundRectangle2D.Double(x, y, width, height, 14, 14);
        box.clip(shape);
        box.setColor(Color.WHITE);
        box.fill(shape);

        // Phần đầu có màu: tên môn và giờ học
        Font bold = new Font("Segoe UI", Font.BOLD, 12);
        Font small = new Font("Segoe UI", Font.PLAIN, 11);
        List<String> title = wrap(box.getFontMetrics(bold), course.name, width - 14);
        List<String> times = wrap(box.getFontMetrics(small), start + " - " + end + period(start), width - 14);
        int headHeight = title.size() * 16 + times.size() * 15 + 10;
        box.setColor(color);
        box.fillRect(x, y, width, headHeight);
        box.setColor(Color.WHITE);
        int line = lines(box, bold, title, x + 7, y + 4, 16);
        lines(box, small, times, x + 7, line, 15);

        // Phần thân màu trắng: mã môn, phòng, giảng viên
        box.setColor(TEXT);
        Font normal = new Font("Segoe UI", Font.PLAIN, 12);
        int bodyY = y + headHeight + 4;
        for (String text : new String[] {course.id + " · " + course.credits + " tín chỉ", course.room,
                course.lecturer}) {
            bodyY = lines(box, normal, wrap(box.getFontMetrics(normal), text, width - 14), x + 7, bodyY, 16);
        }
        box.dispose();

        g2.setColor(new Color(0xD1D5DB));
        g2.draw(shape);
    }

    // Vẽ từng dòng từ trên xuống, trả về vị trí của dòng tiếp theo
    private static int lines(Graphics2D g2, Font font, List<String> texts, int x, int top, int lineHeight) {
        g2.setFont(font);
        for (String text : texts) {
            g2.drawString(text, x, top + g2.getFontMetrics().getAscent());
            top += lineHeight;
        }
        return top;
    }

    // Chia chữ thành nhiều dòng cho vừa bề rộng
    private static List<String> wrap(FontMetrics metrics, String text, int width) {
        List<String> result = new ArrayList<>();
        String current = "";
        for (String word : text.split(" ")) {
            String longer = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && metrics.stringWidth(longer) > width) {
                result.add(current);
                current = word;
            } else {
                current = longer;
            }
        }
        result.add(current);
        return result;
    }
}
