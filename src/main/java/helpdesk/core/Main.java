package helpdesk.core;

import helpdesk.ui.LoginWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Application Entry Point.
 * Sets clean Look and Feel, starts background escalation thread, and shows Login UI.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  Help Desk Ticketing System v1.0         ");
        System.out.println("==========================================");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("Could not set System Look and Feel: {}", e.getMessage());
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
