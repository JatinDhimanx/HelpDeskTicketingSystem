package helpdesk.ui;

import helpdesk.utils.UIConstants;

import javax.swing.*;
import java.awt.*;

/**
 * KPI Stat Card widget for the Reports & Overview dashboard (Task 10).
 */
public class StatCard extends JPanel {

    private final JLabel valueLabel;
    private final JLabel titleLabel;

    public StatCard(String title, String initialValue, Color accentColor) {
        setLayout(new BorderLayout(0, 4));
        setBackground(UIConstants.CARD_COLOR);
        setPreferredSize(new Dimension(160, 95));
        setMinimumSize(new Dimension(140, 90));

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, accentColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                        BorderFactory.createEmptyBorder(12, 16, 12, 16)
                )
        ));

        titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.FONT_SMALL.deriveFont(Font.BOLD));
        titleLabel.setForeground(UIConstants.TEXT_SECONDARY);

        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(UIConstants.TEXT_PRIMARY);

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }
}
