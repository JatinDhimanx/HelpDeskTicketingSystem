package helpdesk.ui;

import helpdesk.models.Ticket;
import helpdesk.models.User;
import helpdesk.services.TicketService;
import helpdesk.utils.UIConstants;
import helpdesk.utils.UIHelper;
import helpdesk.utils.WrapLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Filterable and searchable ticket table panel (Task 06).
 * Asynchronous data retrieval ensures instant UI responsiveness.
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
        setLayout(new BorderLayout(0, 14));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel filterCard = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 8));
        filterCard.setBackground(UIConstants.CARD_COLOR);
        filterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        categoryFilter = new JComboBox<>(new String[]{
                "All", "Technical", "Network", "Hardware", "Software", "Account/Login"
        });
        categoryFilter.setFont(UIConstants.FONT_BODY);
        categoryFilter.setPreferredSize(new Dimension(135, 32));
        categoryFilter.setLightWeightPopupEnabled(false);
        filterCard.add(createFilterGroup("Category:", categoryFilter));

        priorityFilter = new JComboBox<>(new String[]{
                "All", "Critical", "High", "Medium", "Low"
        });
        priorityFilter.setFont(UIConstants.FONT_BODY);
        priorityFilter.setPreferredSize(new Dimension(105, 32));
        priorityFilter.setLightWeightPopupEnabled(false);
        filterCard.add(createFilterGroup("Priority:", priorityFilter));

        statusFilter = new JComboBox<>(new String[]{
                "All", "OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED"
        });
        statusFilter.setFont(UIConstants.FONT_BODY);
        statusFilter.setPreferredSize(new Dimension(120, 32));
        statusFilter.setLightWeightPopupEnabled(false);
        filterCard.add(createFilterGroup("Status:", statusFilter));

        searchField = new JTextField(12);
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(150, 32));
        filterCard.add(createFilterGroup("Search:", searchField));

        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttonGroup.setOpaque(false);

        JButton applyBtn = UIHelper.createStyledButton("Filter", UIConstants.PRIMARY_COLOR, Color.WHITE);
        applyBtn.setPreferredSize(new Dimension(80, 32));
        applyBtn.addActionListener(e -> applyFilters());
        buttonGroup.add(applyBtn);

        JButton resetBtn = new JButton("Reset");
        resetBtn.setFont(UIConstants.FONT_BODY);
        resetBtn.setFocusPainted(false);
        resetBtn.setBackground(Color.WHITE);
        resetBtn.setPreferredSize(new Dimension(75, 32));
        resetBtn.addActionListener(e -> resetFilters());
        buttonGroup.add(resetBtn);

        filterCard.add(buttonGroup);

        searchField.addActionListener(e -> applyFilters());
        add(filterCard, BorderLayout.NORTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                filterCard.revalidate();
                filterCard.repaint();
            }
        });

        String[] columnNames = {"ID", "Title", "Category", "Priority", "Status", "Agent", "Created At", "Resolved At"};
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
        ticketTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        ticketTable.getColumnModel().getColumn(6).setPreferredWidth(140);
        ticketTable.getColumnModel().getColumn(7).setPreferredWidth(140);

        ticketTable.getColumnModel().getColumn(3).setCellRenderer(new PriorityRenderer());
        ticketTable.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());

        JScrollPane scrollPane = new JScrollPane(ticketTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        add(scrollPane, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        countLabel = new JLabel("Loading tickets...");
        countLabel.setFont(UIConstants.FONT_SMALL);
        countLabel.setForeground(UIConstants.TEXT_SECONDARY);
        footerPanel.add(countLabel, BorderLayout.WEST);

        JButton refreshBtn = new JButton("Refresh Data");
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

        countLabel.setText("Applying filters...");

        SwingWorker<List<Ticket>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Ticket> doInBackground() {
                if (showAllTickets) {
                    return ticketService.getFilteredTickets(catParam, priParam, statParam, searchParam);
                } else {
                    List<Ticket> userTickets = ticketService.getTicketsForUser(currentUser.getUserId());
                    return userTickets.stream()
                            .filter(t -> catParam == null || catParam.equalsIgnoreCase(t.getCategory()))
                            .filter(t -> priParam == null || priParam.equalsIgnoreCase(t.getPriority()))
                            .filter(t -> statParam == null || statParam.equalsIgnoreCase(t.getStatus()))
                            .filter(t -> searchParam == null ||
                                    String.valueOf(t.getTicketId()).contains(searchParam) ||
                                    (t.getTitle() != null && t.getTitle().toLowerCase().contains(searchParam.toLowerCase())) ||
                                    (t.getDescription() != null && t.getDescription().toLowerCase().contains(searchParam.toLowerCase())))
                            .toList();
                }
            }

            @Override
            protected void done() {
                try {
                    populateTable(get());
                } catch (Exception ex) {
                    countLabel.setText("Failed to apply filters: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    public void resetFilters() {
        categoryFilter.setSelectedIndex(0);
        priorityFilter.setSelectedIndex(0);
        statusFilter.setSelectedIndex(0);
        searchField.setText("");
        refreshTickets();
    }

    public void refreshTickets() {
        countLabel.setText("Loading tickets from database...");
        SwingWorker<List<Ticket>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Ticket> doInBackground() {
                return showAllTickets
                        ? ticketService.getAllTickets()
                        : ticketService.getTicketsForUser(currentUser.getUserId());
            }

            @Override
            protected void done() {
                try {
                    populateTable(get());
                } catch (Exception ex) {
                    countLabel.setText("Error loading tickets: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void populateTable(List<Ticket> tickets) {
        tableModel.setRowCount(0);
        for (Ticket t : tickets) {
            String agentDisplay = t.getAgentName() != null && !t.getAgentName().isEmpty()
                    ? t.getAgentName()
                    : (t.getAgentId() != null ? "Agent " + t.getAgentId() : "Unassigned");
            tableModel.addRow(new Object[]{
                    t.getTicketId(),
                    t.getTitle(),
                    t.getCategory(),
                    t.getPriority(),
                    t.getStatus(),
                    agentDisplay,
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

    private JPanel createFilterGroup(String labelText, JComponent component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        panel.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD, 12f));
        label.setForeground(UIConstants.TEXT_PRIMARY);
        panel.add(label);
        panel.add(component);
        return panel;
    }
}
