package helpdesk.ui;

import helpdesk.models.Agent;
import helpdesk.services.AgentService;
import helpdesk.utils.UIConstants;
import helpdesk.utils.UIHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Dedicated Management Console for Support Agents.
 * Allows Administrators to view, add, remove, and toggle availability of agents.
 * Auto-suggests sequential agent naming (e.g., Agent 1, Agent 2, Agent 3).
 */
public class ManageAgentsPanel extends JPanel {

    private final AgentService agentService;
    private JTable agentTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton addAgentBtn;
    private JButton removeAgentBtn;
    private JButton toggleAvailBtn;
    private JButton refreshBtn;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ManageAgentsPanel() {
        this.agentService = new AgentService();
        initUI();
        loadAgents();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header Panel
        JPanel topHeader = new JPanel(new BorderLayout(0, 8));
        topHeader.setOpaque(false);

        JPanel titleBox = new JPanel(new BorderLayout(0, 2));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("Support Agent Management");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Add, remove, and monitor support agents across the help desk system.");
        subtitle.setFont(UIConstants.FONT_BODY);
        subtitle.setForeground(UIConstants.TEXT_SECONDARY);
        titleBox.add(title, BorderLayout.NORTH);
        titleBox.add(subtitle, BorderLayout.SOUTH);

        // Action Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        toolbar.setOpaque(false);

        addAgentBtn = UIHelper.createStyledButton("+ Add New Agent", UIConstants.PRIMARY_COLOR, Color.WHITE);
        addAgentBtn.setPreferredSize(new Dimension(150, 36));
        addAgentBtn.addActionListener(e -> showAddAgentDialog());

        removeAgentBtn = new JButton("Remove Agent");
        removeAgentBtn.setFont(UIConstants.FONT_BODY);
        removeAgentBtn.setFocusPainted(false);
        removeAgentBtn.setBackground(new Color(254, 242, 242));
        removeAgentBtn.setForeground(UIConstants.ERROR_COLOR);
        removeAgentBtn.setBorder(BorderFactory.createLineBorder(new Color(252, 165, 165), 1));
        removeAgentBtn.setPreferredSize(new Dimension(130, 36));
        removeAgentBtn.addActionListener(e -> handleRemoveAgent());

        toggleAvailBtn = new JButton("Toggle Availability");
        toggleAvailBtn.setFont(UIConstants.FONT_BODY);
        toggleAvailBtn.setFocusPainted(false);
        toggleAvailBtn.setBackground(Color.WHITE);
        toggleAvailBtn.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        toggleAvailBtn.setPreferredSize(new Dimension(150, 36));
        toggleAvailBtn.addActionListener(e -> handleToggleAvailability());

        refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(UIConstants.FONT_BODY);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBackground(Color.WHITE);
        refreshBtn.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        refreshBtn.setPreferredSize(new Dimension(90, 36));
        refreshBtn.addActionListener(e -> loadAgents());

        toolbar.add(addAgentBtn);
        toolbar.add(removeAgentBtn);
        toolbar.add(toggleAvailBtn);
        toolbar.add(refreshBtn);

        topHeader.add(titleBox, BorderLayout.WEST);
        topHeader.add(toolbar, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        // Agent Table
        String[] columns = {"Agent ID", "Display Name", "Email Address", "Availability", "Active Workload"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        agentTable = new JTable(tableModel);
        UIHelper.styleTable(agentTable);
        agentTable.setRowHeight(34);
        agentTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        agentTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        agentTable.getColumnModel().getColumn(2).setPreferredWidth(240);
        agentTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        agentTable.getColumnModel().getColumn(4).setPreferredWidth(150);

        agentTable.getColumnModel().getColumn(3).setCellRenderer(new AvailabilityPillRenderer());

        JScrollPane scrollPane = new JScrollPane(agentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        add(scrollPane, BorderLayout.CENTER);

        // Footer status bar
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        statusLabel = new JLabel("Loading agents...");
        statusLabel.setFont(UIConstants.FONT_SMALL);
        statusLabel.setForeground(UIConstants.TEXT_SECONDARY);
        footer.add(statusLabel, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);
    }

    public void loadAgents() {
        statusLabel.setText("Fetching agents and workloads from database...");
        setActionsEnabled(false);

        SwingWorker<AgentLoadResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AgentLoadResult doInBackground() {
                List<Agent> agents = agentService.getAllAgents();
                Map<Integer, Integer> workloads = agentService.getAllAgentWorkloads();
                return new AgentLoadResult(agents, workloads);
            }

            @Override
            protected void done() {
                setActionsEnabled(true);
                try {
                    AgentLoadResult res = get();
                    tableModel.setRowCount(0);
                    for (Agent a : res.agents) {
                        int load = res.workloads.getOrDefault(a.getAgentId(), 0);
                        String loadDisplay = load == 1 ? "1 active ticket" : load + " active tickets";
                        String displayName = a.getName() != null && !a.getName().isEmpty() ? a.getName() : "Agent " + a.getAgentId();

                        tableModel.addRow(new Object[]{
                                a.getAgentId(),
                                displayName,
                                a.getEmail(),
                                a.isAvailable() ? "AVAILABLE" : "UNAVAILABLE",
                                loadDisplay
                        });
                    }
                    statusLabel.setText("Total Agents: " + res.agents.size());
                } catch (Exception e) {
                    statusLabel.setText("Error loading agents: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void showAddAgentDialog() {
        // Pre-compute suggestion
        String[] nextSuggestion = agentService.suggestNextAgent();
        String suggestedName = nextSuggestion[0];
        String suggestedEmail = nextSuggestion[1];

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add New Support Agent", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        content.setBackground(UIConstants.CARD_COLOR);

        JLabel dlgTitle = new JLabel("Create Support Agent");
        dlgTitle.setFont(UIConstants.FONT_HEADING);
        dlgTitle.setForeground(UIConstants.PRIMARY_COLOR);
        dlgTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(dlgTitle);
        content.add(Box.createVerticalStrut(16));

        // Name field
        JLabel nameLbl = new JLabel("Agent Display Name:");
        nameLbl.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField nameField = new JTextField(suggestedName);
        nameField.setFont(UIConstants.FONT_BODY);
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(nameLbl);
        content.add(Box.createVerticalStrut(4));
        content.add(nameField);
        content.add(Box.createVerticalStrut(10));

        // Email field
        JLabel emailLbl = new JLabel("Email Address:");
        emailLbl.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        emailLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField emailField = new JTextField(suggestedEmail);
        emailField.setFont(UIConstants.FONT_BODY);
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(emailLbl);
        content.add(Box.createVerticalStrut(4));
        content.add(emailField);
        content.add(Box.createVerticalStrut(10));

        // Password field
        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPasswordField passField = new JPasswordField("admin123");
        passField.setFont(UIConstants.FONT_BODY);
        passField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        passField.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(passLbl);
        content.add(Box.createVerticalStrut(4));
        content.add(passField);
        content.add(Box.createVerticalGlue());

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(UIConstants.FONT_BODY);
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = UIHelper.createStyledButton("Create Agent", UIConstants.PRIMARY_COLOR, Color.WHITE);
        saveBtn.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!email.contains("@") || !email.contains(".")) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid email address.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (password.length() < 4) {
                JOptionPane.showMessageDialog(dialog, "Password must be at least 4 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            saveBtn.setEnabled(false);
            saveBtn.setText("Creating...");

            SwingWorker<Agent, Void> createWorker = new SwingWorker<>() {
                @Override
                protected Agent doInBackground() throws Exception {
                    return agentService.createAgent(name, email, password);
                }

                @Override
                protected void done() {
                    try {
                        Agent created = get();
                        if (created != null) {
                            dialog.dispose();
                            JOptionPane.showMessageDialog(ManageAgentsPanel.this,
                                    "Agent '" + name + "' successfully created!",
                                    "Success",
                                    JOptionPane.INFORMATION_MESSAGE);
                            loadAgents();
                        } else {
                            saveBtn.setEnabled(true);
                            saveBtn.setText("Create Agent");
                            JOptionPane.showMessageDialog(dialog,
                                    "Could not create agent. The email address might already be in use.",
                                    "Creation Failed",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        saveBtn.setEnabled(true);
                        saveBtn.setText("Create Agent");
                        JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            createWorker.execute();
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        content.add(btnPanel);

        dialog.add(content, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void handleRemoveAgent() {
        int selectedRow = agentTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select an agent from the table to remove.",
                    "No Agent Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int agentId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String name = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove " + name + " (ID #" + agentId + ")?\n" +
                "Any tickets currently assigned to this agent will be unassigned and set to OPEN.",
                "Confirm Agent Removal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            statusLabel.setText("Removing " + name + "...");
            setActionsEnabled(false);

            SwingWorker<Boolean, Void> deleteWorker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return agentService.deleteAgent(agentId);
                }

                @Override
                protected void done() {
                    setActionsEnabled(true);
                    try {
                        boolean ok = get();
                        if (ok) {
                            JOptionPane.showMessageDialog(ManageAgentsPanel.this,
                                    name + " was successfully removed.",
                                    "Agent Removed",
                                    JOptionPane.INFORMATION_MESSAGE);
                            loadAgents();
                        } else {
                            JOptionPane.showMessageDialog(ManageAgentsPanel.this,
                                    "Failed to remove agent.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ManageAgentsPanel.this,
                                "Error removing agent: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            deleteWorker.execute();
        }
    }

    private void handleToggleAvailability() {
        int selectedRow = agentTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select an agent to toggle availability.",
                    "No Agent Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int agentId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String currentStatus = (String) tableModel.getValueAt(selectedRow, 3);
        boolean newAvail = !"AVAILABLE".equalsIgnoreCase(currentStatus);

        setActionsEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                agentService.setAgentAvailability(agentId, newAvail);
                return null;
            }

            @Override
            protected void done() {
                loadAgents();
            }
        };
        worker.execute();
    }

    private void setActionsEnabled(boolean enabled) {
        addAgentBtn.setEnabled(enabled);
        removeAgentBtn.setEnabled(enabled);
        toggleAvailBtn.setEnabled(enabled);
        refreshBtn.setEnabled(enabled);
    }

    private static class AgentLoadResult {
        final List<Agent> agents;
        final Map<Integer, Integer> workloads;

        AgentLoadResult(List<Agent> agents, Map<Integer, Integer> workloads) {
            this.agents = agents;
            this.workloads = workloads;
        }
    }

    /**
     * Pill badge renderer for Availability (Green for Available, Gray for Unavailable).
     */
    private static class AvailabilityPillRenderer extends DefaultTableCellRenderer {
        private Color bg = new Color(243, 244, 246);
        private Color fg = new Color(75, 85, 99);

        public AvailabilityPillRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String val = value != null ? value.toString().toUpperCase() : "";
            setFont(new Font("Segoe UI", Font.BOLD, 11));

            if ("AVAILABLE".equals(val)) {
                bg = new Color(209, 250, 229); // soft emerald
                fg = new Color(4, 120, 87);    // deep green
            } else {
                bg = new Color(243, 244, 246); // slate gray
                fg = new Color(107, 114, 128);
            }
            setForeground(fg);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 12;
            int h = getHeight() - 8;
            int w = getWidth() - 16;
            int x = 8;
            int y = 4;

            g2.setColor(bg);
            g2.fillRoundRect(x, y, w, h, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
