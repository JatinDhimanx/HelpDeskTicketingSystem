package helpdesk.ui;

import helpdesk.core.EscalationManager;
import helpdesk.models.User;
import helpdesk.utils.UIConstants;

import javax.swing.*;
import java.awt.*;

/**
 * Admin Central Dashboard with sidebar navigation and full system management (Task 11).
 */
public class AdminDashboard extends JFrame {

    private final User currentUser;
    private final CardLayout cardLayout;
    private final JPanel contentContainer;

    private TicketTablePanel ticketTablePanel;
    private AgentAssignmentPanel agentAssignmentPanel;
    private CreateTicketPanel createTicketPanel;
    private ReportsDashboardPanel reportsPanel;

    private JButton allTicketsNavBtn;
    private JButton assignAgentsNavBtn;
    private JButton createTicketNavBtn;
    private JButton reportsNavBtn;

    public AdminDashboard(User user) {
        super("Help Desk ? Administration Control Center (" + user.getName() + ")");
        this.currentUser = user;
        this.cardLayout = new CardLayout();
        this.contentContainer = new JPanel(this.cardLayout);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(950, 620));
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

        JLabel brand = new JLabel("Help Desk Management Console");
        brand.setFont(UIConstants.FONT_TITLE);
        brand.setForeground(UIConstants.PRIMARY_COLOR);

        JPanel userBadge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        userBadge.setOpaque(false);

        JLabel roleTag = new JLabel("SYSTEM ADMIN");
        roleTag.setFont(UIConstants.FONT_SMALL.deriveFont(Font.BOLD));
        roleTag.setOpaque(true);
        roleTag.setBackground(new Color(254, 226, 226));
        roleTag.setForeground(UIConstants.ERROR_COLOR);
        roleTag.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JLabel userInfo = new JLabel(user.getName() + " (" + user.getEmail() + ")");
        userInfo.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        userInfo.setForeground(UIConstants.TEXT_PRIMARY);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(UIConstants.FONT_SMALL);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBackground(new Color(254, 242, 242));
        logoutBtn.setForeground(UIConstants.ERROR_COLOR);
        logoutBtn.setBorder(BorderFactory.createLineBorder(new Color(252, 165, 165), 1));
        logoutBtn.addActionListener(e -> handleLogout());

        userBadge.add(roleTag);
        userBadge.add(userInfo);
        userBadge.add(logoutBtn);

        topHeader.add(brand, BorderLayout.WEST);
        topHeader.add(userBadge, BorderLayout.EAST);
        root.add(topHeader, BorderLayout.NORTH);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBackground(UIConstants.CARD_COLOR);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UIConstants.BORDER_COLOR),
                BorderFactory.createEmptyBorder(20, 14, 20, 14)
        ));

        JLabel navLabel = new JLabel("ADMINISTRATION");
        navLabel.setFont(UIConstants.FONT_SMALL.deriveFont(Font.BOLD));
        navLabel.setForeground(UIConstants.TEXT_SECONDARY);
        navLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(navLabel);
        sidebar.add(Box.createVerticalStrut(12));

        allTicketsNavBtn = createNavButton("?? All Tickets", true);
        assignAgentsNavBtn = createNavButton("?? Agent Assignment", false);
        createTicketNavBtn = createNavButton("? Create Ticket", false);
        reportsNavBtn = createNavButton("?? Reports & Analytics", false);

        allTicketsNavBtn.addActionListener(e -> {
            selectNav(allTicketsNavBtn);
            ticketTablePanel.refreshTickets();
            cardLayout.show(contentContainer, "ALL_TICKETS");
        });

        assignAgentsNavBtn.addActionListener(e -> {
            selectNav(assignAgentsNavBtn);
            agentAssignmentPanel.loadTickets();
            agentAssignmentPanel.loadAgents();
            cardLayout.show(contentContainer, "ASSIGN_AGENTS");
        });

        createTicketNavBtn.addActionListener(e -> {
            selectNav(createTicketNavBtn);
            cardLayout.show(contentContainer, "CREATE_TICKET");
        });

        reportsNavBtn.addActionListener(e -> {
            selectNav(reportsNavBtn);
            reportsPanel.refreshAllReports();
            cardLayout.show(contentContainer, "REPORTS");
        });

        sidebar.add(allTicketsNavBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(assignAgentsNavBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(createTicketNavBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(reportsNavBtn);
        sidebar.add(Box.createVerticalGlue());

        root.add(sidebar, BorderLayout.WEST);

        contentContainer.setOpaque(false);

        ticketTablePanel = new TicketTablePanel(currentUser, true);
        agentAssignmentPanel = new AgentAssignmentPanel();
        createTicketPanel = new CreateTicketPanel(currentUser, () -> {
            ticketTablePanel.refreshTickets();
            selectNav(allTicketsNavBtn);
            cardLayout.show(contentContainer, "ALL_TICKETS");
        });
        reportsPanel = new ReportsDashboardPanel();

        contentContainer.add(ticketTablePanel, "ALL_TICKETS");
        contentContainer.add(agentAssignmentPanel, "ASSIGN_AGENTS");
        contentContainer.add(createTicketPanel, "CREATE_TICKET");
        contentContainer.add(reportsPanel, "REPORTS");

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
        styleNavBtn(allTicketsNavBtn, activeBtn == allTicketsNavBtn);
        styleNavBtn(assignAgentsNavBtn, activeBtn == assignAgentsNavBtn);
        styleNavBtn(createTicketNavBtn, activeBtn == createTicketNavBtn);
        styleNavBtn(reportsNavBtn, activeBtn == reportsNavBtn);
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of the Admin Console?",
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
