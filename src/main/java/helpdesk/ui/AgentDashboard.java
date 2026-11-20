package helpdesk.ui;

import helpdesk.models.Agent;
import helpdesk.models.User;
import helpdesk.services.AgentService;
import helpdesk.utils.UIConstants;

import javax.swing.*;
import java.awt.*;

/**
 * Agent Dashboard with modern light theme and sidebar navigation (Task 11).
 */
public class AgentDashboard extends JFrame {

    private final User currentUser;
    private final AgentService agentService;
    private Agent agent;

    private final CardLayout cardLayout;
    private final JPanel contentContainer;

    private AgentTicketPanel agentTicketPanel;
    private TicketTablePanel allTicketsPanel;

    private JButton assignedNavBtn;
    private JButton allTicketsNavBtn;
    private JLabel availabilityBadge;
    private JButton toggleAvailBtn;

    public AgentDashboard(User user) {
        super("Help Desk - Agent Workspace (" + user.getName() + ")");
        this.currentUser = user;
        this.agentService = new AgentService();
        this.agent = agentService.getAgentByUserId(user.getUserId()).orElse(null);
        this.cardLayout = new CardLayout();
        this.contentContainer = new JPanel(this.cardLayout);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1140, 720);
        setMinimumSize(new Dimension(850, 540));
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

        JLabel brand = new JLabel("Help Desk Agent Workspace");
        brand.setFont(UIConstants.FONT_TITLE);
        brand.setForeground(UIConstants.PRIMARY_COLOR);

        JPanel userBadge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        userBadge.setOpaque(false);

        availabilityBadge = new JLabel(getAvailabilityText());
        availabilityBadge.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        availabilityBadge.setForeground(getAvailabilityColor());

        toggleAvailBtn = new JButton(agent != null && agent.isAvailable() ? "Go Offline" : "Go Available");
        toggleAvailBtn.setFont(UIConstants.FONT_SMALL);
        toggleAvailBtn.setFocusPainted(false);
        toggleAvailBtn.setBackground(Color.WHITE);
        toggleAvailBtn.addActionListener(e -> toggleAvailability());

        JLabel userInfo = new JLabel(user.getName() + " [AGENT]");
        userInfo.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        userInfo.setForeground(UIConstants.TEXT_PRIMARY);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(UIConstants.FONT_SMALL);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBackground(new Color(254, 242, 242));
        logoutBtn.setForeground(UIConstants.ERROR_COLOR);
        logoutBtn.setBorder(BorderFactory.createLineBorder(new Color(252, 165, 165), 1));
        logoutBtn.addActionListener(e -> handleLogout());

        userBadge.add(availabilityBadge);
        userBadge.add(toggleAvailBtn);
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

        JLabel navLabel = new JLabel("AGENT MENU");
        navLabel.setFont(UIConstants.FONT_SMALL.deriveFont(Font.BOLD));
        navLabel.setForeground(UIConstants.TEXT_SECONDARY);
        navLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(navLabel);
        sidebar.add(Box.createVerticalStrut(12));

        assignedNavBtn = createNavButton("My Assigned Tickets", true);
        allTicketsNavBtn = createNavButton("All System Tickets", false);

        assignedNavBtn.addActionListener(e -> {
            selectNav(assignedNavBtn);
            agentTicketPanel.loadAgentTickets();
            cardLayout.show(contentContainer, "ASSIGNED");
        });

        allTicketsNavBtn.addActionListener(e -> {
            selectNav(allTicketsNavBtn);
            allTicketsPanel.refreshTickets();
            cardLayout.show(contentContainer, "ALL_TICKETS");
        });

        sidebar.add(assignedNavBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(allTicketsNavBtn);
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

        agentTicketPanel = new AgentTicketPanel(currentUser);
        allTicketsPanel = new TicketTablePanel(currentUser, true);

        contentContainer.add(agentTicketPanel, "ASSIGNED");
        contentContainer.add(allTicketsPanel, "ALL_TICKETS");

        root.add(contentContainer, BorderLayout.CENTER);
    }

    private void toggleAvailability() {
        if (agent == null) return;
        boolean newAvail = !agent.isAvailable();
        agentService.setAgentAvailability(agent.getAgentId(), newAvail);
        agent.setAvailable(newAvail);

        availabilityBadge.setText(getAvailabilityText());
        availabilityBadge.setForeground(getAvailabilityColor());
        toggleAvailBtn.setText(newAvail ? "Go Offline" : "Go Available");

        JOptionPane.showMessageDialog(this,
                "Availability status updated to: " + (newAvail ? "ONLINE / AVAILABLE" : "OFFLINE / BUSY"),
                "Availability Changed",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String getAvailabilityText() {
        return agent != null && agent.isAvailable() ? "? Online / Available" : "? Offline / Busy";
    }

    private Color getAvailabilityColor() {
        return agent != null && agent.isAvailable() ? UIConstants.SUCCESS_COLOR : UIConstants.ERROR_COLOR;
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
        styleNavBtn(assignedNavBtn, activeBtn == assignedNavBtn);
        styleNavBtn(allTicketsNavBtn, activeBtn == allTicketsNavBtn);
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of the Agent Workspace?",
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
