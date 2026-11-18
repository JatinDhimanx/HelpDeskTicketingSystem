package helpdesk.ui;

import helpdesk.models.Agent;
import helpdesk.models.Ticket;
import helpdesk.services.AgentService;
import helpdesk.services.TicketService;
import helpdesk.utils.UIConstants;
import helpdesk.utils.UIHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Admin Ticket Assignment & Dispatch Module (Task 07).
 * Fast asynchronous data loading.
 */
public class AgentAssignmentPanel extends JPanel {

    private final TicketService ticketService;
    private final AgentService agentService;

    private JTable ticketTable;
    private DefaultTableModel tableModel;
    private JLabel selectedTicketTitleLabel;
    private JLabel selectedTicketStatusLabel;
    private JComboBox<AgentItem> agentCombo;
    private JLabel workloadLabel;
    private JLabel availabilityLabel;
    private JButton assignButton;
    private Integer currentSelectedTicketId;

    public AgentAssignmentPanel() {
        this.ticketService = new TicketService();
        this.agentService = new AgentService();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel title = new JLabel("Agent Assignment & Ticket Dispatch");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Select a ticket to review current status and dispatch it to an active agent.");
        subtitle.setFont(UIConstants.FONT_BODY);
        subtitle.setForeground(UIConstants.TEXT_SECONDARY);

        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(subtitle, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(580);
        splitPane.setResizeWeight(0.65);
        splitPane.setBorder(null);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
        leftPanel.setBackground(UIConstants.CARD_COLOR);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);
        JLabel tableTitle = new JLabel("All Open & Assigned Tickets");
        tableTitle.setFont(UIConstants.FONT_HEADING);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(UIConstants.FONT_SMALL);
        refreshBtn.addActionListener(e -> {
            loadTickets();
            loadAgents();
        });
        tableHeader.add(tableTitle, BorderLayout.WEST);
        tableHeader.add(refreshBtn, BorderLayout.EAST);
        leftPanel.add(tableHeader, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Category", "Priority", "Status", "Agent"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ticketTable = new JTable(tableModel);
        UIHelper.styleTable(ticketTable);
        ticketTable.setRowHeight(30);
        ticketTable.getColumnModel().getColumn(3).setCellRenderer(new PriorityRenderer());
        ticketTable.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        ticketTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onTicketSelected();
            }
        });

        leftPanel.add(new JScrollPane(ticketTable), BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(UIConstants.CARD_COLOR);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel assignHeader = new JLabel("Assign / Reassign Ticket");
        assignHeader.setFont(UIConstants.FONT_HEADING);
        assignHeader.setForeground(UIConstants.TEXT_PRIMARY);
        assignHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        selectedTicketTitleLabel = new JLabel("Select a ticket from the left list");
        selectedTicketTitleLabel.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        selectedTicketTitleLabel.setForeground(UIConstants.PRIMARY_COLOR);
        selectedTicketTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        selectedTicketStatusLabel = new JLabel("Status: -");
        selectedTicketStatusLabel.setFont(UIConstants.FONT_SMALL);
        selectedTicketStatusLabel.setForeground(UIConstants.TEXT_SECONDARY);
        selectedTicketStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel agentLabel = new JLabel("Select Support Agent:");
        agentLabel.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        agentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        agentCombo = new JComboBox<>();
        agentCombo.setFont(UIConstants.FONT_BODY);
        agentCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        agentCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        agentCombo.addActionListener(e -> updateAgentWorkloadDisplay());

        workloadLabel = new JLabel("Current Active Workload: -");
        workloadLabel.setFont(UIConstants.FONT_BODY);
        workloadLabel.setForeground(UIConstants.TEXT_SECONDARY);
        workloadLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        availabilityLabel = new JLabel("Status: -");
        availabilityLabel.setFont(UIConstants.FONT_SMALL);
        availabilityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        assignButton = UIHelper.createStyledButton("Assign Ticket", UIConstants.PRIMARY_COLOR, Color.WHITE);
        assignButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        assignButton.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        assignButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        assignButton.setEnabled(false);
        assignButton.addActionListener(e -> handleAssign());

        rightPanel.add(assignHeader);
        rightPanel.add(Box.createVerticalStrut(14));
        rightPanel.add(new JLabel("Selected Ticket:"));
        rightPanel.add(Box.createVerticalStrut(4));
        rightPanel.add(selectedTicketTitleLabel);
        rightPanel.add(selectedTicketStatusLabel);
        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(agentLabel);
        rightPanel.add(Box.createVerticalStrut(6));
        rightPanel.add(agentCombo);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(workloadLabel);
        rightPanel.add(Box.createVerticalStrut(4));
        rightPanel.add(availabilityLabel);
        rightPanel.add(Box.createVerticalStrut(24));
        rightPanel.add(assignButton);
        rightPanel.add(Box.createVerticalGlue());

        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);
    }

    public void loadTickets() {
        SwingWorker<List<Ticket>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Ticket> doInBackground() {
                return ticketService.getAllTickets();
            }

            @Override
            protected void done() {
                try {
                    List<Ticket> tickets = get();
                    tableModel.setRowCount(0);
                    for (Ticket t : tickets) {
                        String agentDisplay = t.getAgentId() != null ? "Agent #" + t.getAgentId() : "Unassigned";
                        tableModel.addRow(new Object[]{
                                t.getTicketId(),
                                t.getTitle(),
                                t.getCategory(),
                                t.getPriority(),
                                t.getStatus(),
                                agentDisplay
                        });
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    public void loadAgents() {
        SwingWorker<List<Agent>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Agent> doInBackground() {
                return agentService.getAllAgents();
            }

            @Override
            protected void done() {
                try {
                    List<Agent> agents = get();
                    agentCombo.removeAllItems();
                    for (Agent a : agents) {
                        agentCombo.addItem(new AgentItem(a));
                    }
                    updateAgentWorkloadDisplay();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void onTicketSelected() {
        int row = ticketTable.getSelectedRow();
        if (row >= 0) {
            currentSelectedTicketId = (Integer) tableModel.getValueAt(row, 0);
            String title = (String) tableModel.getValueAt(row, 1);
            String status = (String) tableModel.getValueAt(row, 4);
            selectedTicketTitleLabel.setText("#" + currentSelectedTicketId + " - " + title);
            selectedTicketStatusLabel.setText("Current Status: " + status);
            assignButton.setEnabled(true);
        } else {
            currentSelectedTicketId = null;
            selectedTicketTitleLabel.setText("Select a ticket from the left list");
            selectedTicketStatusLabel.setText("Status: -");
            assignButton.setEnabled(false);
        }
    }

    private void updateAgentWorkloadDisplay() {
        AgentItem item = (AgentItem) agentCombo.getSelectedItem();
        if (item != null) {
            Agent agent = item.getAgent();
            workloadLabel.setText("Loading workload...");

            SwingWorker<Integer, Void> worker = new SwingWorker<>() {
                @Override
                protected Integer doInBackground() {
                    return agentService.getAgentWorkload(agent.getAgentId());
                }

                @Override
                protected void done() {
                    try {
                        int workload = get();
                        workloadLabel.setText("Current Active Workload: " + workload + " ticket(s)");
                        if (agent.isAvailable()) {
                            availabilityLabel.setText("? Available for assignment");
                            availabilityLabel.setForeground(UIConstants.SUCCESS_COLOR);
                            if (currentSelectedTicketId != null) assignButton.setEnabled(true);
                        } else {
                            availabilityLabel.setText("? Unavailable (Offline / On Leave)");
                            availabilityLabel.setForeground(UIConstants.ERROR_COLOR);
                            assignButton.setEnabled(false);
                        }
                    } catch (Exception ignored) {}
                }
            };
            worker.execute();
        } else {
            workloadLabel.setText("Current Active Workload: -");
            availabilityLabel.setText("Status: -");
        }
    }

    private void handleAssign() {
        if (currentSelectedTicketId == null) {
            JOptionPane.showMessageDialog(this, "Please select a ticket first.", "No Ticket Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AgentItem item = (AgentItem) agentCombo.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Please select an agent.", "No Agent Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Agent agent = item.getAgent();
        if (!agent.isAvailable()) {
            JOptionPane.showMessageDialog(this,
                    "Cannot assign: Agent " + agent.getName() + " is marked as unavailable.",
                    "Agent Unavailable",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Assign Ticket #" + currentSelectedTicketId + " to agent " + agent.getName() + " (" + agent.getEmail() + ")?",
                "Confirm Assignment",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            assignButton.setEnabled(false);
            assignButton.setText("Assigning...");

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    Ticket ticket = ticketService.getTicketById(currentSelectedTicketId);
                    agentService.assignTicketToAgent(agent, ticket);
                    return null;
                }

                @Override
                protected void done() {
                    assignButton.setEnabled(true);
                    assignButton.setText("Assign Ticket");
                    try {
                        get();
                        JOptionPane.showMessageDialog(AgentAssignmentPanel.this,
                                "Ticket #" + currentSelectedTicketId + " assigned successfully to " + agent.getName() + "!",
                                "Assignment Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        loadTickets();
                        updateAgentWorkloadDisplay();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AgentAssignmentPanel.this,
                                "Assignment failed: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    public static class AgentItem {
        private final Agent agent;

        public AgentItem(Agent agent) {
            this.agent = agent;
        }

        public Agent getAgent() {
            return agent;
        }

        @Override
        public String toString() {
            String avail = agent.isAvailable() ? "Available" : "Unavailable";
            return agent.getName() + " (" + agent.getEmail() + ") - [" + avail + "]";
        }
    }
}
