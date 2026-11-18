package helpdesk.ui;

import helpdesk.utils.UIConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Modern pill badge cell renderer for ticket status values (Task 06 / Task 08 / Task 11).
 */
public class StatusRenderer extends DefaultTableCellRenderer {

    private Color badgeBg = new Color(243, 244, 246);
    private Color badgeFg = new Color(75, 85, 99);

    public StatusRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER);
        setOpaque(false);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        String status = value != null ? value.toString().toUpperCase() : "";
        setFont(new Font("Segoe UI", Font.BOLD, 11));

        switch (status) {
            case "OPEN" -> {
                badgeBg = new Color(219, 234, 254); // soft sky blue
                badgeFg = new Color(29, 78, 216);   // strong blue
            }
            case "ASSIGNED" -> {
                badgeBg = new Color(237, 233, 254); // soft violet
                badgeFg = new Color(109, 40, 217);  // purple
            }
            case "IN_PROGRESS" -> {
                badgeBg = new Color(254, 243, 199); // soft amber
                badgeFg = new Color(180, 83, 9);    // deep amber
            }
            case "RESOLVED" -> {
                badgeBg = new Color(209, 250, 229); // soft emerald
                badgeFg = new Color(4, 120, 87);    // deep green
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
        int pillWidth = Math.min(width - 10, 96);
        int x = (width - pillWidth) / 2;
        int y = (height - pillHeight) / 2;

        g2.setColor(badgeBg);
        g2.fillRoundRect(x, y, pillWidth, pillHeight, pillHeight, pillHeight);

        g2.dispose();
        super.paintComponent(g);
    }
}
