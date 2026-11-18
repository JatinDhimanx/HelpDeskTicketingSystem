package helpdesk.ui;

import helpdesk.models.Ticket;
import helpdesk.models.User;
import helpdesk.services.TicketService;
import helpdesk.utils.UIConstants;
import helpdesk.utils.UIHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Filterable and searchable ticket table panel (Task 06).
 */
public class TicketTablePanel extends JPanel {

    private final User currentUser;
    private final boolean showAllTickets;
    private final TicketService ticketService;

    private JTable ticketTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> categoryFilter;
    private JComboBox<String> priorityFilter;
    private JComboBox<String> statusFilter;
    private JTextField searchField;
    private JLabel countLabel;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public TicketTablePanel(User currentUser, boolean showAllTickets) {
        this.currentUser = currentUser;
        this.showAllTickets = showAllTickets;
        this.ticketService = new TicketService();
        initUI();
        refreshTickets();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        filterCard.setBackground(UIConstants.CARD_COLOR);
        filterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        filterCard.add(new JLabel("Category:"));
        categoryFilter = new JComboBox<>(new String[]{
                "All", "Technical", "Network", "Hardware", "Software", "Account/Login"
        });
        filterCard.add(categoryFilter);

        filterCard.add(new JLabel("Priority:"));
        priorityFilter = new JComboBox<>(new String[]{
                "All", "Critical", "High", "Medium", "Low"
        });
        filterCard.add(priorityFilter);

        filterCard.add(new JLabel("Status:"));
        statusFilter = new JComboBox<>(new String[]{
                "All", "OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED"
        });
        filterCard.add(statusFilter);

        filterCard.add(new JLabel("Search (ID / Title):"));
        searchField = new JTextField(14);
        filterCard.add(searchField);

        JButton applyBtn = UIHelper.createStyledButton("Filter", UIConstants.PRIMARY_COLOR, Color.WHITE);
        applyBtn.setPreferredSize(new Dimension(80, 30));
        applyBtn.addActionListener(e -> applyFilters());
        filterCard.add(applyBtn);

        JButton resetBtn = new JButton("Reset");
        resetBtn.setFont(UIConstants.FONT_BODY);
        resetBtn.setFocusPainted(false);
        resetBtn.setBackground(Color.WHITE);
        resetBtn.addActionListener(e -> resetFilters());
        filterCard.add(resetBtn);

        searchField.addActionListener(e -> applyFilters());
        add(filterCard, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Title", "Category", "Priority", "Status", "Agent ID", "Created At", "Resolved At"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ticketTable = new JTable(tableModel);
        UIHelper.styleTable(ticketTable);
        ticketTable.setRowHeight(32);
        ticketTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        ticketTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        ticketTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        ticketTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        ticketTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        ticketTable.getColumnModel().getColumn(5).setPreferredWidth(70);
        ticketTable.getColumnModel().getColumn(6).setPreferredWidth(140);
        ticketTable.getColumnModel().getColumn(7).setPreferredWidth(140);

        ticketTable.getColumnModel().getColumn(3).setCellRenderer(new PriorityRenderer());

        JScrollPane scrollPane = new JScrollPane(ticketTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        add(scrollPane, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        countLabel = new JLabel("Showing 0 tickets");
        countLabel.setFont(UIConstants.FONT_SMALL);
        countLabel.setForeground(UIConstants.TEXT_SECONDARY);
        footerPanel.add(countLabel, BorderLayout.WEST);

        JButton refreshBtn = new JButton("?? Refresh Data");
        refreshBtn.setFont(UIConstants.FONT_SMALL);
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> refreshTickets());
        footerPanel.add(refreshBtn, BorderLayout.EAST);

        add(footerPanel, BorderLayout.SOUTH);
    }

    public void applyFilters() {
        String cat = (String) categoryFilter.getSelectedItem();
        String pri = (String) priorityFilter.getSelectedItem();
        String stat = (String) statusFilter.getSelectedItem();
        String search = searchField.getText().trim();

        String catParam = "All".equalsIgnoreCase(cat) ? null : cat;
        String priParam = "All".equalsIgnoreCase(pri) ? null : pri;
        String statParam = "All".equalsIgnoreCase(stat) ? null : stat;
        String searchParam = search.isEmpty() ? null : search;

        List<Ticket> filtered;
        if (showAllTickets) {
            filtered = ticketService.getFilteredTickets(catParam, priParam, statParam, searchParam);
        } else {
            List<Ticket> userTickets = ticketService.getTicketsForUser(currentUser.getUserId());
            filtered = userTickets.stream()
                    .filter(t -> catParam == null || catParam.equalsIgnoreCase(t.getCategory()))
                    .filter(t -> priParam == null || priParam.equalsIgnoreCase(t.getPriority()))
                    .filter(t -> statParam == null || statParam.equalsIgnoreCase(t.getStatus()))
                    .filter(t -> searchParam == null ||
                            String.valueOf(t.getTicketId()).contains(searchParam) ||
                            (t.getTitle() != null && t.getTitle().toLowerCase().contains(searchParam.toLowerCase())) ||
                            (t.getDescription() != null && t.getDescription().toLowerCase().contains(searchParam.toLowerCase())))
                    .toList();
        }

        populateTable(filtered);
    }

    public void resetFilters() {
        categoryFilter.setSelectedIndex(0);
        priorityFilter.setSelectedIndex(0);
        statusFilter.setSelectedIndex(0);
        searchField.setText("");
        refreshTickets();
    }

    public void refreshTickets() {
        List<Ticket> tickets = showAllTickets
                ? ticketService.getAllTickets()
                : ticketService.getTicketsForUser(currentUser.getUserId());
        populateTable(tickets);
    }

    private void populateTable(List<Ticket> tickets) {
        tableModel.setRowCount(0);
        for (Ticket t : tickets) {
            tableModel.addRow(new Object[]{
                    t.getTicketId(),
                    t.getTitle(),
                    t.getCategory(),
                    t.getPriority(),
                    t.getStatus(),
                    t.getAgentId() != null ? "#" + t.getAgentId() : "Unassigned",
                    t.getCreatedAt() != null ? dateFormat.format(t.getCreatedAt()) : "-",
                    t.getResolvedAt() != null ? dateFormat.format(t.getResolvedAt()) : "-"
            });
        }
        countLabel.setText("Showing " + tickets.size() + " ticket(s)");
    }

    public JTable getTable() {
        return ticketTable;
    }

    public Integer getSelectedTicketId() {
        int row = ticketTable.getSelectedRow();
        if (row >= 0) {
            return (Integer) tableModel.getValueAt(row, 0);
        }
        return null;
    }
}
