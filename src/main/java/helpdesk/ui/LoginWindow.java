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
 * Professional login window for the Help Desk Ticketing System.
 * Authenticates users asynchronously and routes them to the appropriate dashboard.
 */
public class LoginWindow extends JFrame {

    private JTextField     emailField;
    private JPasswordField passwordField;
    private JLabel         emailError;
    private JLabel         passwordError;
    private JButton        loginBtn;

    private final AuthService authService = new AuthService();

    public LoginWindow() {
        super("Help Desk System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(440, 520);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIConstants.BACKGROUND_COLOR);
        setContentPane(root);

        JPanel banner = new JPanel();
        banner.setBackground(UIConstants.PRIMARY_COLOR);
        banner.setBorder(BorderFactory.createEmptyBorder(28, 20, 28, 20));
        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));

        JLabel systemTitle = new JLabel("Help Desk System", SwingConstants.CENTER);
        systemTitle.setFont(UIConstants.FONT_TITLE);
        systemTitle.setForeground(Color.WHITE);
        systemTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in to your account", SwingConstants.CENTER);
        subtitle.setFont(UIConstants.FONT_BODY);
        subtitle.setForeground(new Color(191, 219, 254));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        banner.add(systemTitle);
        banner.add(Box.createVerticalStrut(6));
        banner.add(subtitle);
        root.add(banner, BorderLayout.NORTH);

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.CARD_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                        UIConstants.BORDER_RADIUS * 2, UIConstants.BORDER_RADIUS * 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                new DropShadowBorder(),
                BorderFactory.createEmptyBorder(28, 36, 28, 36)
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

        JPanel passwordRow = new JPanel(new BorderLayout(4, 0));
        passwordRow.setOpaque(false);
        passwordRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        passwordField = new JPasswordField();
        styleTextField(passwordField);
        passwordField.setEchoChar('?');

        JButton toggleBtn = new JButton("Show");
        toggleBtn.setFont(UIConstants.FONT_SMALL);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        toggleBtn.setBackground(Color.WHITE);
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleBtn.addActionListener(e -> {
            if (passwordField.getEchoChar() == '\u0000') {
                passwordField.setEchoChar('?');
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

        loginBtn = UIHelper.createStyledButton("Login", UIConstants.PRIMARY_COLOR, Color.WHITE);
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginBtn.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD, 14f));
        loginBtn.addActionListener(e -> handleLogin());

        ActionListener loginAction = e -> handleLogin();
        emailField.addActionListener(loginAction);
        passwordField.addActionListener(loginAction);

        card.add(emailLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(emailField);
        card.add(emailError);
        card.add(Box.createVerticalStrut(14));
        card.add(passwordLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(passwordRow);
        card.add(passwordError);
        card.add(Box.createVerticalStrut(24));
        card.add(loginBtn);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIConstants.BACKGROUND_COLOR);
        center.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        center.add(card);
        root.add(center, BorderLayout.CENTER);

        JLabel footer = new JLabel("? 2024 Help Desk System. All rights reserved.", SwingConstants.CENTER);
        footer.setFont(UIConstants.FONT_SMALL);
        footer.setForeground(UIConstants.TEXT_SECONDARY);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        root.add(footer, BorderLayout.SOUTH);
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
                    loginBtn.setText("Login");

                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : "Invalid credentials";

                    UIManager.put("OptionPane.background",        new Color(254, 242, 242));
                    UIManager.put("Panel.background",             new Color(254, 242, 242));
                    UIManager.put("OptionPane.messageForeground", UIConstants.ERROR_COLOR);
                    JOptionPane.showMessageDialog(
                            LoginWindow.this,
                            msg,
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                    UIManager.put("OptionPane.background",        null);
                    UIManager.put("Panel.background",             null);
                    UIManager.put("OptionPane.messageForeground", null);
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
        JLabel lbl = new JLabel("? " + message);
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
                int alpha = 15 - i * 3;
                g2.setColor(new Color(0, 0, 0, Math.max(alpha, 0)));
                g2.drawRoundRect(x + i, y + i, width - i * 2 - 1, height - i * 2 - 1,
                        UIConstants.BORDER_RADIUS * 2, UIConstants.BORDER_RADIUS * 2);
            }
            g2.dispose();
        }
    }
}
