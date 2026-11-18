package helpdesk.ui;

import helpdesk.models.User;
import helpdesk.services.AuthService;
import helpdesk.utils.UIConstants;
import helpdesk.utils.UIHelper;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Modern, responsive login window for the Help Desk Ticketing System (Task 04).
 * Features quick-fill demo buttons, password visibility toggle, and asynchronous authentication.
 */
public class LoginWindow extends JFrame {

    private JTextField     emailField;
    private JPasswordField passwordField;
    private JLabel         emailError;
    private JLabel         passwordError;
    private JButton        loginBtn;

    private final AuthService authService = new AuthService();

    public LoginWindow() {
        super("Help Desk System - Secure Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(480, 600);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIConstants.BACKGROUND_COLOR);
        setContentPane(root);

        // Header Banner
        JPanel banner = new JPanel();
        banner.setBackground(UIConstants.PRIMARY_COLOR);
        banner.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));
        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));

        JLabel systemTitle = new JLabel("Help Desk Ticketing System", SwingConstants.CENTER);
        systemTitle.setFont(UIConstants.FONT_TITLE.deriveFont(Font.BOLD, 22f));
        systemTitle.setForeground(Color.WHITE);
        systemTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in to your support portal", SwingConstants.CENTER);
        subtitle.setFont(UIConstants.FONT_BODY);
        subtitle.setForeground(new Color(191, 219, 254));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        banner.add(systemTitle);
        banner.add(Box.createVerticalStrut(4));
        banner.add(subtitle);
        root.add(banner, BorderLayout.NORTH);

        // Main Login Card
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.CARD_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                new DropShadowBorder(),
                BorderFactory.createEmptyBorder(24, 32, 24, 32)
        ));

        JLabel emailLabel = new JLabel("Email Address");
        emailLabel.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        emailLabel.setForeground(UIConstants.TEXT_PRIMARY);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        emailField = new JTextField();
        styleTextField(emailField);

        emailError = createErrorLabel("Please enter your email address.");
        emailError.setVisible(false);

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        passwordLabel.setForeground(UIConstants.TEXT_PRIMARY);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel passwordRow = new JPanel(new BorderLayout(6, 0));
        passwordRow.setOpaque(false);
        passwordRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        passwordField = new JPasswordField();
        styleTextField(passwordField);
        passwordField.setEchoChar('\u2022');

        JButton toggleBtn = new JButton("Show");
        toggleBtn.setFont(UIConstants.FONT_SMALL);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setBackground(new Color(243, 244, 246));
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleBtn.addActionListener(e -> {
            if (passwordField.getEchoChar() == '\u0000') {
                passwordField.setEchoChar('\u2022');
                toggleBtn.setText("Show");
            } else {
                passwordField.setEchoChar('\u0000');
                toggleBtn.setText("Hide");
            }
        });

        passwordRow.add(passwordField, BorderLayout.CENTER);
        passwordRow.add(toggleBtn, BorderLayout.EAST);

        passwordError = createErrorLabel("Please enter your password.");
        passwordError.setVisible(false);

        loginBtn = UIHelper.createStyledButton("Sign In", UIConstants.PRIMARY_COLOR, Color.WHITE);
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginBtn.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD, 14f));
        loginBtn.addActionListener(e -> handleLogin());

        ActionListener loginAction = e -> handleLogin();
        emailField.addActionListener(loginAction);
        passwordField.addActionListener(loginAction);

        // Quick demo login bar
        JPanel demoDivider = new JPanel(new BorderLayout());
        demoDivider.setOpaque(false);
        demoDivider.setAlignmentX(Component.LEFT_ALIGNMENT);
        demoDivider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel demoHint = new JLabel("DEMO ACCOUNTS (1-CLICK FILL)", SwingConstants.CENTER);
        demoHint.setFont(new Font("Segoe UI", Font.BOLD, 10));
        demoHint.setForeground(UIConstants.TEXT_SECONDARY);
        demoDivider.add(demoHint, BorderLayout.CENTER);

        JPanel demoRow = new JPanel(new GridLayout(1, 3, 8, 0));
        demoRow.setOpaque(false);
        demoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        demoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JButton adminQuick = createQuickLoginButton("Admin", "admin@helpdesk.com", "admin123");
        JButton agentQuick = createQuickLoginButton("Agent", "agent1@helpdesk.com", "admin123");
        JButton userQuick  = createQuickLoginButton("User", "user1@helpdesk.com", "admin123");

        demoRow.add(adminQuick);
        demoRow.add(agentQuick);
        demoRow.add(userQuick);

        // Assemble card
        card.add(emailLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(emailField);
        card.add(emailError);
        card.add(Box.createVerticalStrut(12));
        card.add(passwordLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(passwordRow);
        card.add(passwordError);
        card.add(Box.createVerticalStrut(18));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(18));
        card.add(demoDivider);
        card.add(Box.createVerticalStrut(8));
        card.add(demoRow);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIConstants.BACKGROUND_COLOR);
        center.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        center.add(card);
        root.add(center, BorderLayout.CENTER);

        // Footer status bar
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
        footerPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        JLabel footer = new JLabel("Help Desk System v1.0  |  Cloud MySQL Connected");
        footer.setFont(UIConstants.FONT_SMALL);
        footer.setForeground(UIConstants.TEXT_SECONDARY);
        footerPanel.add(footer);
        root.add(footerPanel, BorderLayout.SOUTH);
    }

    private JButton createQuickLoginButton(String label, String email, String password) {
        JButton btn = new JButton(label);
        btn.setFont(UIConstants.FONT_SMALL.deriveFont(Font.BOLD));
        btn.setFocusPainted(false);
        btn.setBackground(new Color(241, 245, 249));
        btn.setForeground(UIConstants.TEXT_PRIMARY);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            emailField.setText(email);
            passwordField.setText(password);
            emailError.setVisible(false);
            passwordError.setVisible(false);
            handleLogin();
        });
        return btn;
    }

    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        boolean valid = true;

        if (email.isEmpty()) {
            emailError.setVisible(true);
            emailField.setBorder(BorderFactory.createLineBorder(UIConstants.ERROR_COLOR, 1));
            valid = false;
        } else {
            emailError.setVisible(false);
            styleTextField(emailField);
        }

        if (password.isEmpty()) {
            passwordError.setVisible(true);
            passwordField.setBorder(BorderFactory.createLineBorder(UIConstants.ERROR_COLOR, 1));
            valid = false;
        } else {
            passwordError.setVisible(false);
            styleTextField(passwordField);
        }

        if (!valid) return;

        loginBtn.setEnabled(false);
        loginBtn.setText("Signing in...");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                return authService.authenticate(email, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    openDashboard(user);
                } catch (Exception ex) {
                    loginBtn.setEnabled(true);
                    loginBtn.setText("Sign In");

                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : "Invalid credentials";

                    JOptionPane.showMessageDialog(
                            LoginWindow.this,
                            msg,
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private void openDashboard(User user) {
        dispose();
        SwingUtilities.invokeLater(() -> {
            switch (user.getRole()) {
                case "ADMIN" -> {
                    AdminDashboard admin = new AdminDashboard(user);
                    admin.setVisible(true);
                }
                case "AGENT" -> {
                    AgentDashboard agent = new AgentDashboard(user);
                    agent.setVisible(true);
                }
                default -> {
                    UserDashboard userDash = new UserDashboard(user);
                    userDash.setVisible(true);
                }
            }
        });
    }

    private static void styleTextField(JComponent field) {
        field.setFont(UIConstants.FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        field.setBackground(Color.WHITE);
        if (field instanceof JTextField tf) {
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        }
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static JLabel createErrorLabel(String message) {
        JLabel lbl = new JLabel("! " + message);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(UIConstants.ERROR_COLOR);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private static class DropShadowBorder extends AbstractBorder {
        private static final int SHADOW = 4;

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(SHADOW, SHADOW, SHADOW, SHADOW);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 1; i <= SHADOW; i++) {
                int alpha = 14 - i * 3;
                g2.setColor(new Color(0, 0, 0, Math.max(alpha, 0)));
                g2.drawRoundRect(x + i, y + i, width - i * 2 - 1, height - i * 2 - 1, 16, 16);
            }
            g2.dispose();
        }
    }
}
