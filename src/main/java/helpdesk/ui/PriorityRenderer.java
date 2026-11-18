package helpdesk.ui;

import helpdesk.utils.UIConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Modern pill badge cell renderer for ticket priority values (Task 06 / Task 11).
 */
public class PriorityRenderer extends DefaultTableCellRenderer {

    private Color badgeBg = new Color(243, 244, 246);
    private Color badgeFg = new Color(75, 85, 99);

    public PriorityRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER);
        setOpaque(false);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        String priority = value != null ? value.toString().toUpperCase() : "";
        setFont(new Font("Segoe UI", Font.BOLD, 11));

        switch (priority) {
            case "CRITICAL" -> {
                badgeBg = new Color(254, 226, 226); // soft red
                badgeFg = new Color(185, 28, 28);   // deep red
            }
            case "HIGH" -> {
                badgeBg = new Color(255, 237, 213); // soft orange
                badgeFg = new Color(194, 65, 12);   // deep orange
            }
            case "MEDIUM" -> {
                badgeBg = new Color(254, 243, 199); // soft amber
                badgeFg = new Color(180, 83, 9);    // deep amber
            }
            case "LOW" -> {
                badgeBg = new Color(220, 252, 231); // soft green
                badgeFg = new Color(21, 128, 61);   // deep green
            }
            default -> {
                badgeBg = new Color(243, 244, 246);
                badgeFg = new Color(75, 85, 99);
            }
        }

        setForeground(badgeFg);
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        int pillHeight = Math.min(22, Math.max(16, height - 8));
        int pillWidth = Math.min(width - 10, 88);
        int x = (width - pillWidth) / 2;
        int y = (height - pillHeight) / 2;

        g2.setColor(badgeBg);
        g2.fillRoundRect(x, y, pillWidth, pillHeight, pillHeight, pillHeight);

        g2.dispose();
        super.paintComponent(g);
    }
}
