package main;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;

// Thời khóa biểu: cột là thứ, hàng là buổi, mỗi môn đã đăng ký là một ô màu
class TimetablePanel extends JPanel {
    private static final Color NAVY = new Color(0x102A4F);
    private static final Color LIGHT = new Color(0xE8F2FD);
    private static final Color LINE = new Color(0xD5E0F0);
    private static final Color[] COLORS = {NAVY, new Color(0xBE185D), new Color(0x0F766E),
            new Color(0xB45309), new Color(0x6D28D9), new Color(0x1D4ED8), new Color(0x15803D),
            new Color(0xB91C1C)};
    private static final String[] DAYS = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"};
    private static final String[] DAY_NAMES = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};

    private final JPanel sheet = new JPanel(new BorderLayout(0, 8)); // phần được in
    private final JLabel lblTitle = label("Thời khóa biểu · Học kỳ 1 · Năm học 2026–2027", Font.BOLD, 14, NAVY);
    private final JPanel grid = new JPanel(new GridBagLayout());

    TimetablePanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JButton btnPrint = new JButton("In thời khóa biểu");
        btnPrint.addActionListener(e -> print());
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.add(label("Môn trùng lịch bị chặn ngay khi đăng ký.", Font.PLAIN, 12, new Color(0x6B7280)),
                BorderLayout.CENTER);
        top.add(btnPrint, BorderLayout.EAST);

        grid.setBackground(Color.WHITE);
        sheet.setBackground(Color.WHITE);
        sheet.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        sheet.add(lblTitle, BorderLayout.NORTH);
        sheet.add(grid, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(sheet), BorderLayout.CENTER);
    }

    // Vẽ lại lưới theo danh sách môn đã đăng ký
    void showCourses(String studentName, List<SwingApp.Course> registered) {
        lblTitle.setText("Thời khóa biểu · " + studentName + " · Học kỳ 1 · Năm học 2026–2027");
        grid.removeAll();

        addCell(headerCell("Buổi", LIGHT, NAVY), 0, 0, 0);
        for (int i = 0; i < DAY_NAMES.length; i++) {
            addCell(headerCell(DAY_NAMES[i], NAVY, Color.WHITE), i + 1, 0, 0);
        }

        String[] periods = {"Buổi sáng", "Buổi chiều"};
        for (int row = 0; row < periods.length; row++) {
            addCell(headerCell(periods[row], LIGHT, NAVY), 0, row + 1, 1);
            for (int day = 0; day < DAYS.length; day++) {
                addCell(slot(registered, DAYS[day], row == 0), day + 1, row + 1, 1);
            }
        }
        grid.revalidate();
        grid.repaint();
    }

    // Một ô của lưới: các môn học vào thứ này, trong buổi sáng hoặc buổi chiều
    private JPanel slot(List<SwingApp.Course> registered, String day, boolean morning) {
        List<SwingApp.Course> lessons = new ArrayList<>();
        for (SwingApp.Course course : registered) {
            // Lịch có dạng "Thứ 2 08:00–10:00"
            int split = course.schedule.lastIndexOf(' ');
            boolean startsInMorning = course.schedule.substring(split + 1).compareTo("12:00") < 0;
            if (course.schedule.substring(0, split).equals(day) && startsInMorning == morning) {
                lessons.add(course);
            }
        }
        lessons.sort(Comparator.comparing(course -> course.schedule));

        JPanel slot = new JPanel();
        slot.setLayout(new BoxLayout(slot, BoxLayout.Y_AXIS));
        slot.setBackground(new Color(0xFBFDFF));
        slot.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        for (SwingApp.Course course : lessons) {
            JLabel box = label("<html><body style='width:95px'><b>" + course.id + " · " + course.name
                    + "</b><br>" + course.schedule.substring(course.schedule.lastIndexOf(' ') + 1)
                    + "<br>Phòng " + course.room + "<br>GV: " + course.lecturer + "</body></html>",
                    Font.PLAIN, 11, Color.WHITE);
            box.setOpaque(true);
            box.setBackground(COLORS[registered.indexOf(course) % COLORS.length]);
            box.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            box.setMaximumSize(new Dimension(Integer.MAX_VALUE, box.getPreferredSize().height));
            box.setAlignmentX(LEFT_ALIGNMENT);
            slot.add(box);
            slot.add(Box.createVerticalStrut(4));
        }
        return slot;
    }

    private static JLabel headerCell(String text, Color background, Color foreground) {
        JLabel label = label(text, Font.BOLD, 12, foreground);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(background);
        label.setBorder(BorderFactory.createEmptyBorder(9, 4, 9, 4));
        return label;
    }

    private void addCell(JComponent cell, int column, int row, double weightY) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row;
        c.weightx = column == 0 ? 0 : 1;
        c.weighty = weightY;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);
        // Các cột thứ rộng bằng nhau, dù ô có môn hay không
        if (column > 0) cell.setPreferredSize(new Dimension(130,
                row == 0 ? cell.getPreferredSize().height : Math.max(110, cell.getPreferredSize().height)));
        grid.add(cell, c);
    }

    private static JLabel label(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    // In lưới ra giấy ngang, tự thu nhỏ cho vừa một trang
    private void print() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Thời khóa biểu");
        PageFormat page = job.defaultPage();
        page.setOrientation(PageFormat.LANDSCAPE);
        job.setPrintable((graphics, format, index) -> {
            if (index > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(format.getImageableX(), format.getImageableY());
            double scale = Math.min(format.getImageableWidth() / sheet.getWidth(),
                    format.getImageableHeight() / sheet.getHeight());
            g2.scale(scale, scale);
            sheet.printAll(g2);
            return Printable.PAGE_EXISTS;
        }, page);
        if (!job.printDialog()) return;
        try {
            job.print();
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "Không in được: " + e.getMessage());
        }
    }
}
