package helpdesk.ui;

import helpdesk.models.User;
import helpdesk.utils.UIConstants;

import javax.swing.*;
import java.awt.*;

/**
 * User Dashboard with modern light theme and sidebar navigation (Task 11).
 */
public class UserDashboard extends JFrame {

    private final User currentUser;
    private final CardLayout cardLayout;
    private final JPanel contentContainer;

    private UserTicketPanel userTicketPanel;
    private CreateTicketPanel createTicketPanel;

    private JButton myTicketsNavBtn;
    private JButton createTicketNavBtn;

    public UserDashboard(User user) {
        super("Help Desk - User Portal (" + user.getName() + ")");
        this.currentUser = user;
        this.cardLayout = new CardLayout();
        this.contentContainer = new JPanel(this.cardLayout);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 680);
        setMinimumSize(new Dimension(800, 520));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIConstants.BACKGROUND_COLOR);
        setContentPane(root);

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setBackground(UIConstants.CARD_COLOR);
        topHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));

        JLabel brand = new JLabel("Help Desk Portal");
        brand.setFont(UIConstants.FONT_TITLE);
        brand.setForeground(UIConstants.PRIMARY_COLOR);

        JPanel userBadge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        userBadge.setOpaque(false);
        JLabel userInfo = new JLabel(user.getName() + " (" + user.getEmail() + ")  [USER]");
        userInfo.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        userInfo.setForeground(UIConstants.TEXT_PRIMARY);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(UIConstants.FONT_SMALL);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBackground(new Color(254, 242, 242));
        logoutBtn.setForeground(UIConstants.ERROR_COLOR);
        logoutBtn.setBorder(BorderFactory.createLineBorder(new Color(252, 165, 165), 1));
        logoutBtn.addActionListener(e -> handleLogout());

        userBadge.add(userInfo);
        userBadge.add(logoutBtn);

        topHeader.add(brand, BorderLayout.WEST);
        topHeader.add(userBadge, BorderLayout.EAST);
        root.add(topHeader, BorderLayout.NORTH);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(UIConstants.CARD_COLOR);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UIConstants.BORDER_COLOR),
                BorderFactory.createEmptyBorder(20, 14, 20, 14)
        ));

        JLabel navLabel = new JLabel("NAVIGATION");
        navLabel.setFont(UIConstants.FONT_SMALL.deriveFont(Font.BOLD));
        navLabel.setForeground(UIConstants.TEXT_SECONDARY);
        navLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(navLabel);
        sidebar.add(Box.createVerticalStrut(12));

        myTicketsNavBtn = createNavButton("My Tickets", true);
        createTicketNavBtn = createNavButton("Create Ticket", false);

        myTicketsNavBtn.addActionListener(e -> {
            selectNav(myTicketsNavBtn);
            userTicketPanel.loadTickets();
            cardLayout.show(contentContainer, "MY_TICKETS");
        });

        createTicketNavBtn.addActionListener(e -> {
            selectNav(createTicketNavBtn);
            cardLayout.show(contentContainer, "CREATE_TICKET");
        });

        sidebar.add(myTicketsNavBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(createTicketNavBtn);
        sidebar.add(Box.createVerticalGlue());

        JPanel systemStatus = new JPanel();
        systemStatus.setLayout(new BoxLayout(systemStatus, BoxLayout.Y_AXIS));
        systemStatus.setBackground(new Color(248, 250, 252));
        systemStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        systemStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        systemStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel statusDb = new JLabel("Database: Connected");
        statusDb.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusDb.setForeground(new Color(22, 101, 52));
        systemStatus.add(statusDb);

        sidebar.add(systemStatus);

        root.add(sidebar, BorderLayout.WEST);

        contentContainer.setOpaque(false);

        userTicketPanel = new UserTicketPanel(currentUser);
        createTicketPanel = new CreateTicketPanel(currentUser, () -> {
            userTicketPanel.loadTickets();
            selectNav(myTicketsNavBtn);
            cardLayout.show(contentContainer, "MY_TICKETS");
        });

        contentContainer.add(userTicketPanel, "MY_TICKETS");
        contentContainer.add(createTicketPanel, "CREATE_TICKET");

        root.add(contentContainer, BorderLayout.CENTER);
    }

    private JButton createNavButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        styleNavBtn(btn, active);
        return btn;
    }

    private void styleNavBtn(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(new Color(239, 246, 255));
            btn.setForeground(UIConstants.PRIMARY_COLOR);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, UIConstants.PRIMARY_COLOR),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
        } else {
            btn.setBackground(UIConstants.CARD_COLOR);
            btn.setForeground(UIConstants.TEXT_PRIMARY);
            btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 12));
        }
    }

    private void selectNav(JButton activeBtn) {
        styleNavBtn(myTicketsNavBtn, activeBtn == myTicketsNavBtn);
        styleNavBtn(createTicketNavBtn, activeBtn == createTicketNavBtn);
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginWindow().setVisible(true));
        }
    }
}
