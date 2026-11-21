package helpdesk.core;

import com.formdev.flatlaf.FlatLightLaf;
import helpdesk.ui.LoginWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Application Entry Point.
 * Sets modern FlatLaf Look and Feel, starts background escalation daemon, and displays Login UI.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  Help Desk Ticketing System v1.0         ");
        System.out.println("==========================================");

        try {
            // Apply FlatLaf Modern Look & Feel
            FlatLightLaf.setup();

            // Refined enterprise UI properties
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("CheckBox.arc", 6);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 8);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));

            // Ensure combo box popups and menus are always top-level and never render behind other components
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        } catch (Throwable t) {
            logger.warn("FlatLaf setup failed, falling back to system Look and Feel: {}", t.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                logger.warn("Could not set System Look and Feel: {}", e.getMessage());
            }
        }

        // Pre-warm DB connection pool and user authentication cache asynchronously in background
        try {
            helpdesk.database.DBConnection.getInstance().warmUp();
            new helpdesk.services.AuthService().preWarm();
        } catch (Throwable t) {
            logger.warn("Pre-warm initialization skipped: {}", t.getMessage());
        }

        try {
            EscalationManager.start();
        } catch (Exception e) {
            logger.error("Failed to start EscalationManager: {}", e.getMessage());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Application shutting down. Stopping background threads...");
            EscalationManager.stop();
        }, "ShutdownHookThread"));

        SwingUtilities.invokeLater(() -> {
            try {
                LoginWindow loginWindow = new LoginWindow();
                loginWindow.setVisible(true);
            } catch (Exception e) {
                logger.error("Fatal error launching Login UI: {}", e.getMessage(), e);
                JOptionPane.showMessageDialog(null,
                        "Failed to launch Help Desk application: " + e.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
