package main;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;
import javax.swing.*;

// Tab thời khóa biểu: dòng ghi chú, nút in và lịch tuần (CalendarGrid)
class TimetablePanel extends JPanel {
    private static final Color NAVY = new Color(0x102A4F);

    private final JPanel sheet = new JPanel(new BorderLayout(0, 8)); // phần được in
    private final JLabel lblTitle = label("Thời khóa biểu · Học kỳ 1 · Năm học 2026–2027", Font.BOLD, 14, NAVY);
    private final CalendarGrid grid = new CalendarGrid();

    TimetablePanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JButton btnPrint = new JButton("In thời khóa biểu");
        btnPrint.addActionListener(e -> print());
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.add(label("Môn trùng lịch bị chặn ngay khi đăng ký.", Font.PLAIN, 12, new Color(0x6B7280)),
                BorderLayout.CENTER);
        top.add(btnPrint, BorderLayout.EAST);

        sheet.setBackground(Color.WHITE);
        sheet.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        sheet.add(lblTitle, BorderLayout.NORTH);
        sheet.add(grid, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(sheet);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);
    }

    // Vẽ lại lịch theo danh sách môn đã đăng ký
    void showCourses(String studentName, List<SwingApp.Course> registered) {
        lblTitle.setText("Thời khóa biểu · " + studentName + " · Học kỳ 1 · Năm học 2026–2027");
        grid.setCourses(registered);
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
