package helpdesk.ui;

import helpdesk.utils.UIConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * TableCellRenderer displaying ticket priority values with visual badge indicators (Task 06).
 */
public class PriorityRenderer extends DefaultTableCellRenderer {

    public PriorityRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setOpaque(true);
        label.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD, 12f));

        String priority = value != null ? value.toString() : "";
        Color fgColor;
        Color bgColor;

        switch (priority.toUpperCase()) {
            case "CRITICAL" -> {
                bgColor = new Color(254, 226, 226);
                fgColor = UIConstants.PRIORITY_CRITICAL;
            }
            case "HIGH" -> {
                bgColor = new Color(255, 237, 213);
                fgColor = UIConstants.PRIORITY_HIGH;
            }
            case "MEDIUM" -> {
                bgColor = new Color(254, 243, 199);
                fgColor = UIConstants.PRIORITY_MEDIUM;
            }
            case "LOW" -> {
                bgColor = new Color(220, 252, 231);
                fgColor = UIConstants.PRIORITY_LOW;
            }
            default -> {
                bgColor = Color.WHITE;
                fgColor = UIConstants.TEXT_PRIMARY;
            }
        }

        if (isSelected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            label.setBackground(bgColor);
            label.setForeground(fgColor);
        }

        label.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return label;
    }
}
