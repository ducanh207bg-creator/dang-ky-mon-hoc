package main;

import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.*;

// Các thành phần bo góc và biểu tượng dùng cho màn hình đăng nhập
class LoginParts {

    // Bật khử răng cưa để đường cong được mịn
    private static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g2;
    }

    // Khung nền bo góc
    static class RoundPanel extends JPanel {
        private final int arc;

        RoundPanel(LayoutManager layout, Color background, int arc) {
            super(layout);
            this.arc = arc;
            setBackground(background);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
        }
    }

    // Nhãn nhỏ hình viên thuốc, ví dụ Student Portal
    static JLabel badge(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(background);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setForeground(foreground);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        return label;
    }

    // Vẽ viền bo tròn và chữ gợi ý cho ô nhập
    private static void paintBorder(Graphics g, JComponent field) {
        Graphics2D g2 = smooth(g);
        g2.setColor(field.hasFocus() ? new Color(0x2866A9) : new Color(0xD5D8DD));
        g2.drawRoundRect(0, 0, field.getWidth() - 1, field.getHeight() - 1,
                field.getHeight() - 1, field.getHeight() - 1);
        g2.dispose();
    }

    private static void paintHint(Graphics g, JComponent field, String hint) {
        Graphics2D g2 = smooth(g);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(0xA9AEB6));
        g2.setFont(field.getFont());
        FontMetrics metrics = g2.getFontMetrics();
        int y = (field.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.drawString(hint, field.getInsets().left, y);
        g2.dispose();
    }

    private static void style(JTextField field) {
        field.setOpaque(false);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
    }

    static JTextField textField(String hint) {
        JTextField field = new JTextField(28) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getDocument().getLength() == 0) paintHint(g, this, hint);
                LoginParts.paintBorder(g, this);
            }
        };
        style(field);
        return field;
    }

    static JPasswordField passwordField(String hint) {
        JPasswordField field = new JPasswordField(28) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getDocument().getLength() == 0) paintHint(g, this, hint);
                LoginParts.paintBorder(g, this);
            }
        };
        style(field);
        return field;
    }

    // Nút lớn bo góc
    static JButton button(String text, Color background) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(getModel().isPressed() ? background.darker() : background);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 18));
        return button;
    }

    // Biểu tượng mũ tốt nghiệp trên nền trắng
    static class CapIcon implements Icon {
        public int getIconWidth() {
            return 38;
        }

        public int getIconHeight() {
            return 38;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = smooth(g);
            g2.translate(x, y);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, 38, 38, 18, 18);
            g2.setColor(new Color(0x102A4F));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D top = new Path2D.Double();
            top.moveTo(7, 16);
            top.lineTo(19, 10);
            top.lineTo(31, 16);
            top.lineTo(19, 22);
            top.closePath();
            g2.draw(top);
            Path2D bottom = new Path2D.Double();
            bottom.moveTo(12, 19);
            bottom.lineTo(12, 25);
            bottom.curveTo(15, 29, 23, 29, 26, 25);
            bottom.lineTo(26, 19);
            g2.draw(bottom);
            g2.drawLine(31, 16, 31, 23);
            g2.dispose();
        }
    }

    // Biểu tượng người dùng trong vòng tròn
    static class UserIcon implements Icon {
        public int getIconWidth() {
            return 32;
        }

        public int getIconHeight() {
            return 32;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = smooth(g);
            g2.translate(x, y);
            g2.setColor(new Color(0x2A72C3));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(3, 3, 26, 26);
            g2.drawOval(12, 9, 8, 8);
            g2.drawArc(8, 20, 16, 12, 25, 130);
            g2.dispose();
        }
    }
}
