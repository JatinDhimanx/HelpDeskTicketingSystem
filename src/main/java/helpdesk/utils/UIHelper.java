package helpdesk.utils;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;

public class UIHelper {

    private UIHelper() {}

    /**
     * Creates a styled JButton with a flat, rounded look and hand cursor.
     */
    public static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), UIConstants.BORDER_RADIUS * 2, UIConstants.BORDER_RADIUS * 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 36));

        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (bg.equals(UIConstants.PRIMARY_COLOR)) {
                    btn.setBackground(UIConstants.PRIMARY_HOVER);
                } else {
                    btn.setBackground(bg.darker());
                }
                btn.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
                btn.repaint();
            }
        });
        return btn;
    }

    /**
     * Creates a rounded compound border.
     */
    public static Border createRoundedBorder(int radius, Color color) {
        return BorderFactory.createCompoundBorder(
            new Border() {
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);
                    g2.drawRoundRect(x, y, width - 1, height - 1, radius * 2, radius * 2);
                    g2.dispose();
                }
                @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
                @Override public boolean isBorderOpaque() { return false; }
            },
            BorderFactory.createEmptyBorder(UIConstants.PADDING - 1, UIConstants.PADDING - 1,
                                            UIConstants.PADDING - 1, UIConstants.PADDING - 1)
        );
    }

    /**
     * Creates a header panel with a styled title label.
     */
    public static JPanel createHeaderPanel(String title) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, UIConstants.PADDING, UIConstants.PADDING / 2));
        panel.setBackground(UIConstants.PRIMARY_COLOR);
        JLabel lbl = new JLabel(title);
        lbl.setFont(UIConstants.FONT_TITLE);
        lbl.setForeground(Color.WHITE);
        panel.add(lbl);
        return panel;
    }

    /**
     * Creates a labeled form field panel: label above, field below.
     */
    public static JPanel createFormField(String label, JComponent field) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_BODY);
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height + 6));

        panel.add(lbl);
        panel.add(Box.createVerticalStrut(4));
        panel.add(field);
        return panel;
    }

    /**
     * Styles a JTable with header colors, row height, and grid color.
     */
    public static void styleTable(JTable table) {
        table.setFont(UIConstants.FONT_BODY);
        table.setRowHeight(32);
        table.setGridColor(UIConstants.BORDER_COLOR);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(UIConstants.TEXT_PRIMARY);

        JTableHeader header = table.getTableHeader();
        header.setBackground(UIConstants.PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        // Alternating row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 250, 251));
                    setForeground(UIConstants.TEXT_PRIMARY);
                }
                return this;
            }
        });
    }
}
