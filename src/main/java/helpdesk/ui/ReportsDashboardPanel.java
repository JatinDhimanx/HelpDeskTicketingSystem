package helpdesk.ui;

import helpdesk.models.Ticket;
import helpdesk.services.ReportService;
import helpdesk.utils.UIConstants;
import helpdesk.utils.UIHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Resolution Reports & Analytics Dashboard (Task 10).
 */
public class ReportsDashboardPanel extends JPanel {

    private final ReportService reportService;

    private StatCard totalCard;
    private StatCard openCard;
    private StatCard assignedCard;
    private StatCard inProgressCard;
    private StatCard resolvedCard;
    private JLabel avgResolutionLabel;

    private DefaultTableModel categoryTableModel;
    private DefaultTableModel priorityTableModel;
    private DefaultTableModel agentTableModel;
    private DefaultTableModel pendingTableModel;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ReportsDashboardPanel() {
        this.reportService = new ReportService();
        initUI();
        refreshAllReports();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JPanel titleBox = new JPanel(new BorderLayout(0, 2));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("Help Desk Resolution Reports & Analytics");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Live database statistics, resolution metrics, and workload distribution.");
        subtitle.setFont(UIConstants.FONT_BODY);
        subtitle.setForeground(UIConstants.TEXT_SECONDARY);
        titleBox.add(title, BorderLayout.NORTH);
        titleBox.add(subtitle, BorderLayout.SOUTH);

        JButton refreshBtn = UIHelper.createStyledButton("?? Refresh Reports", UIConstants.PRIMARY_COLOR, Color.WHITE);
        refreshBtn.setPreferredSize(new Dimension(160, 36));
        refreshBtn.addActionListener(e -> refreshAllReports());

        topHeader.add(titleBox, BorderLayout.WEST);
        topHeader.add(refreshBtn, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 16));
        centerPanel.setOpaque(false);

        JPanel kpiContainer = new JPanel(new BorderLayout(0, 8));
        kpiContainer.setOpaque(false);

        JPanel cardsRow = new JPanel(new GridLayout(1, 5, 12, 0));
        cardsRow.setOpaque(false);

        totalCard = new StatCard("Total Tickets", "0", UIConstants.PRIMARY_COLOR);
        openCard = new StatCard("Open Tickets", "0", UIConstants.PRIORITY_MEDIUM);
        assignedCard = new StatCard("Assigned", "0", new Color(14, 165, 233));
        inProgressCard = new StatCard("In Progress", "0", UIConstants.PRIORITY_HIGH);
        resolvedCard = new StatCard("Resolved", "0", UIConstants.SUCCESS_COLOR);

        cardsRow.add(totalCard);
        cardsRow.add(openCard);
        cardsRow.add(assignedCard);
        cardsRow.add(inProgressCard);
        cardsRow.add(resolvedCard);
        kpiContainer.add(cardsRow, BorderLayout.CENTER);

        avgResolutionLabel = new JLabel("Average Resolution Time: calculating...", SwingConstants.RIGHT);
        avgResolutionLabel.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        avgResolutionLabel.setForeground(UIConstants.PRIMARY_COLOR);
        kpiContainer.add(avgResolutionLabel, BorderLayout.SOUTH);

        centerPanel.add(kpiContainer, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));

        tabbedPane.addTab("By Category", createCategoryTab());
        tabbedPane.addTab("By Priority", createPriorityTab());
        tabbedPane.addTab("Agent Performance", createAgentPerformanceTab());
        tabbedPane.addTab("Pending Tickets Queue", createPendingTicketsTab());

        centerPanel.add(tabbedPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createCategoryTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.CARD_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] cols = {"Category Name", "Ticket Count", "Percentage of Total"};
        categoryTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(categoryTableModel);
        UIHelper.styleTable(table);
        table.setRowHeight(32);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPriorityTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.CARD_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] cols = {"Priority Level", "Ticket Count", "Percentage of Total"};
        priorityTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(priorityTableModel);
        UIHelper.styleTable(table);
        table.setRowHeight(32);
        table.getColumnModel().getColumn(0).setCellRenderer(new PriorityRenderer());
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAgentPerformanceTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.CARD_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] cols = {"Agent ID", "Agent Name", "Email Address", "Resolved Tickets"};
        agentTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(agentTableModel);
        UIHelper.styleTable(table);
        table.setRowHeight(32);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPendingTicketsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.CARD_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] cols = {"ID", "Title", "Category", "Priority", "Status", "Agent ID", "Age / Created At"};
        pendingTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(pendingTableModel);
        UIHelper.styleTable(table);
        table.setRowHeight(30);
        table.getColumnModel().getColumn(3).setCellRenderer(new PriorityRenderer());
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    public void refreshAllReports() {
        int total = reportService.getTotalTicketCount();
        int open = reportService.getTicketCountByStatus("OPEN");
        int assigned = reportService.getTicketCountByStatus("ASSIGNED");
        int inProgress = reportService.getTicketCountByStatus("IN_PROGRESS");
        int resolved = reportService.getTicketCountByStatus("RESOLVED");

        totalCard.setValue(String.valueOf(total));
        openCard.setValue(String.valueOf(open));
        assignedCard.setValue(String.valueOf(assigned));
        inProgressCard.setValue(String.valueOf(inProgress));
        resolvedCard.setValue(String.valueOf(resolved));

        double avgHours = reportService.getAverageResolutionTimeHours();
        avgResolutionLabel.setText("? Average Resolution Time: " + avgHours + " hours (" + Math.round(avgHours / 24.0 * 10.0) / 10.0 + " days)");

        categoryTableModel.setRowCount(0);
        Map<String, Integer> catMap = reportService.getTicketsByCategory();
        for (Map.Entry<String, Integer> e : catMap.entrySet()) {
            double pct = total > 0 ? (e.getValue() * 100.0 / total) : 0.0;
            categoryTableModel.addRow(new Object[]{
                    e.getKey(),
                    e.getValue(),
                    String.format("%.1f %%", pct)
            });
        }

        priorityTableModel.setRowCount(0);
        Map<String, Integer> priMap = reportService.getTicketsByPriority();
        for (Map.Entry<String, Integer> e : priMap.entrySet()) {
            double pct = total > 0 ? (e.getValue() * 100.0 / total) : 0.0;
            priorityTableModel.addRow(new Object[]{
                    e.getKey(),
                    e.getValue(),
                    String.format("%.1f %%", pct)
            });
        }

        agentTableModel.setRowCount(0);
        List<ReportService.AgentResolvedStats> agentStats = reportService.getAgentResolvedCounts();
        for (ReportService.AgentResolvedStats s : agentStats) {
            agentTableModel.addRow(new Object[]{
                    "#" + s.getAgentId(),
                    s.getName(),
                    s.getEmail(),
                    s.getResolvedCount() + " closed"
            });
        }

        pendingTableModel.setRowCount(0);
        List<Ticket> pending = reportService.getPendingTickets();
        for (Ticket t : pending) {
            pendingTableModel.addRow(new Object[]{
                    t.getTicketId(),
                    t.getTitle(),
                    t.getCategory(),
                    t.getPriority(),
                    t.getStatus(),
                    t.getAgentId() != null ? "#" + t.getAgentId() : "Unassigned",
                    t.getCreatedAt() != null ? dateFormat.format(t.getCreatedAt()) : "-"
            });
        }
    }
}
