package main;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

// Vẽ một ô của bảng môn học: hai dòng chữ, thanh sĩ số, nhãn trạng thái hoặc nút thao tác
class CourseCell extends JComponent implements TableCellRenderer {
    // Thứ tự các cột trong bảng
    static final int COURSE = 0;
    static final int CREDITS = 1;
    static final int LECTURER = 2;
    static final int SCHEDULE = 3;
    static final int ROOM = 4;
    static final int ENROLLMENT = 5;
    static final int STATUS = 6;
    static final int ACTION = 7;

    private static final Font NORMAL = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Color TEXT = new Color(0x1B2740);
    private static final Color GRAY = new Color(0x6B7280);
    private static final Color LINE = new Color(0xEEF1F5);
    private static final Color SELECTED = new Color(0xF0F6FF);
    private static final Color BLUE = new Color(0x2F7DE9);
    private static final Color RED = new Color(0xC22F2F);

    private SwingApp.Course course;
    private int column;
    private boolean selected;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int viewColumn) {
        course = (SwingApp.Course) value;
        column = table.convertColumnIndexToModel(viewColumn);
        selected = isSelected;
        // Rê chuột vào tên dài để xem đầy đủ
        setToolTipText(column == COURSE ? course.id + " – " + course.name : null);
        return this;
    }

    // Vị trí nút bên trong ô thao tác, dùng cả khi vẽ và khi bắt cú bấm chuột
    static Rectangle buttonBounds(int cellWidth, int cellHeight) {
        return new Rectangle(6, (cellHeight - 32) / 2, Math.min(104, cellWidth - 12), 32);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(selected ? SELECTED : Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(LINE);
        g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

        boolean full = course.enrolled >= course.capacity;
        int middle = getHeight() / 2;
        // Lịch có dạng "Thứ 2 08:00–10:00": phần sau dấu cách cuối là giờ học
        int split = course.schedule.lastIndexOf(' ');

        switch (column) {
            case COURSE:
                text(g2, course.id, BOLD, TEXT, 10, middle - 4);
                text(g2, course.name, NORMAL, TEXT, 10, middle + 15);
                break;
            case CREDITS:
                text(g2, String.valueOf(course.credits), NORMAL, TEXT, 10, middle + 5);
                break;
            case LECTURER:
                text(g2, course.lecturer, NORMAL, TEXT, 10, middle + 5);
                break;
            case SCHEDULE:
                text(g2, course.schedule.substring(0, split), BOLD, TEXT, 10, middle - 4);
                text(g2, course.schedule.substring(split + 1), SMALL, GRAY, 10, middle + 15);
                break;
            case ROOM:
                text(g2, course.room, NORMAL, TEXT, 10, middle + 5);
                break;
            case ENROLLMENT:
                paintEnrollment(g2, middle);
                break;
            case STATUS:
                if (course.registered) {
                    pill(g2, "Đã đăng ký", new Color(0xDCEAFF), new Color(0x1D5FC4), middle);
                } else if (full) {
                    pill(g2, "Hết chỗ", new Color(0xFDE4E4), RED, middle);
                } else {
                    pill(g2, "Còn chỗ", new Color(0xDCF5E6), new Color(0x137A43), middle);
                }
                break;
            case ACTION:
                if (course.registered) {
                    button(g2, "× Hủy môn", Color.WHITE, new Color(0xF0B4B4), RED);
                } else if (full) {
                    button(g2, "Hết chỗ", LINE, LINE, new Color(0x8B94A3));
                } else {
                    button(g2, "+ Đăng ký", BLUE, BLUE, Color.WHITE);
                }
                break;
            default:
                break;
        }
        g2.dispose();
    }

    // Vẽ chữ, nếu dài hơn ô thì cắt bớt và thêm dấu ba chấm
    private void text(Graphics2D g2, String value, Font font, Color color, int x, int y) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics metrics = g2.getFontMetrics();
        int room = getWidth() - x - 4;
        String shown = value;
        if (metrics.stringWidth(value) > room) {
            int length = value.length();
            while (length > 0 && metrics.stringWidth(value.substring(0, length) + "…") > room) length--;
            shown = value.substring(0, length) + "…";
        }
        g2.drawString(shown, x, y);
    }

    // Sĩ số và thanh màu: cam khi sắp đầy, đỏ khi đã đầy
    private void paintEnrollment(Graphics2D g2, int middle) {
        text(g2, course.enrolled + " / " + course.capacity, BOLD, TEXT, 10, middle);
        double ratio = Math.min(1.0, (double) course.enrolled / course.capacity);
        int width = Math.max(20, getWidth() - 24);
        g2.setColor(new Color(0xE8EBF0));
        g2.fillRoundRect(10, middle + 8, width, 6, 6, 6);
        if (ratio > 0) {
            g2.setColor(ratio >= 1 ? RED : ratio >= 0.9 ? new Color(0xD97706) : BLUE);
            g2.fillRoundRect(10, middle + 8, Math.max(6, (int) (width * ratio)), 6, 6, 6);
        }
    }

    private void pill(Graphics2D g2, String value, Color background, Color foreground, int middle) {
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        int width = g2.getFontMetrics().stringWidth(value) + 20;
        g2.setColor(background);
        g2.fillRoundRect(8, middle - 12, width, 24, 24, 24);
        g2.setColor(foreground);
        g2.drawString(value, 18, middle + 4);
    }

    private void button(Graphics2D g2, String value, Color background, Color border, Color foreground) {
        Rectangle box = buttonBounds(getWidth(), getHeight());
        g2.setColor(background);
        g2.fillRoundRect(box.x, box.y, box.width, box.height, 14, 14);
        g2.setColor(border);
        g2.drawRoundRect(box.x, box.y, box.width - 1, box.height - 1, 14, 14);
        g2.setFont(BOLD);
        g2.setColor(foreground);
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(value, box.x + (box.width - metrics.stringWidth(value)) / 2,
                box.y + (box.height - metrics.getHeight()) / 2 + metrics.getAscent());
    }
}
