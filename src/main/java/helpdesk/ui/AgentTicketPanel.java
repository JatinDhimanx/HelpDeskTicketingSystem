package helpdesk.ui;

import helpdesk.models.Agent;
import helpdesk.models.Ticket;
import helpdesk.models.TicketNote;
import helpdesk.models.User;
import helpdesk.services.AgentService;
import helpdesk.services.TicketService;
import helpdesk.utils.UIConstants;
import helpdesk.utils.UIHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Interface for support agents to manage their assigned tickets (Task 08).
 */
public class AgentTicketPanel extends JPanel {

    private final User currentUser;
    private final TicketService ticketService;
    private final AgentService agentService;
    private Agent currentAgent;

    private JTable ticketTable;
    private DefaultTableModel tableModel;
    private JLabel detailTitleLabel;
    private JTextArea descriptionArea;
    private JComboBox<String> statusCombo;
    private JTextArea noteArea;
    private JTextArea notesHistoryArea;
    private JButton updateStatusBtn;
    private JButton addNoteBtn;
    private JButton markResolvedBtn;

    private Integer selectedTicketId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public AgentTicketPanel(User currentUser) {
        this.currentUser = currentUser;
        this.ticketService = new TicketService();
        this.agentService = new AgentService();
        this.currentAgent = agentService.getAgentByUserId(currentUser.getUserId()).orElse(null);

        initUI();
        loadAgentTickets();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel title = new JLabel("Agent Ticket Management");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.TEXT_PRIMARY);

        String displayName = (currentAgent != null && currentAgent.getName() != null && !currentAgent.getName().isEmpty())
                ? currentAgent.getName()
                : (currentAgent != null ? "Agent " + currentAgent.getAgentId() : currentUser.getName());
        String agentInfo = "Logged in as " + displayName + " (" + currentUser.getEmail() + ")";
        JLabel subtitle = new JLabel(agentInfo + " - Update ticket statuses, append resolution progress and close tickets.");
        subtitle.setFont(UIConstants.FONT_BODY);
        subtitle.setForeground(UIConstants.TEXT_SECONDARY);

        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(subtitle, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(520);
        splitPane.setResizeWeight(0.55);
        splitPane.setBorder(null);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
        leftPanel.setBackground(UIConstants.CARD_COLOR);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);
        JLabel tableTitle = new JLabel("My Assigned Tickets");
        tableTitle.setFont(UIConstants.FONT_HEADING);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(UIConstants.FONT_SMALL);
        refreshBtn.addActionListener(e -> loadAgentTickets());
        tableHeader.add(tableTitle, BorderLayout.WEST);
        tableHeader.add(refreshBtn, BorderLayout.EAST);
        leftPanel.add(tableHeader, BorderLayout.NORTH);

        String[] cols = { "ID", "Title", "Category", "Priority", "Status", "Created At" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
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

        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(UIConstants.CARD_COLOR);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        JPanel detailHeader = new JPanel(new BorderLayout(0, 4));
        detailHeader.setOpaque(false);
        detailTitleLabel = new JLabel("Select a ticket to review details");
        detailTitleLabel.setFont(UIConstants.FONT_HEADING);
        detailTitleLabel.setForeground(UIConstants.PRIMARY_COLOR);
        detailHeader.add(detailTitleLabel, BorderLayout.NORTH);

        descriptionArea = new JTextArea(4, 30);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(UIConstants.FONT_BODY);
        descriptionArea.setBackground(new Color(249, 250, 251));
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(BorderFactory.createTitledBorder("Description"));
        detailHeader.add(descScroll, BorderLayout.CENTER);

        rightPanel.add(detailHeader, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        JPanel statusActionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        statusActionRow.setOpaque(false);
        statusActionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusActionRow.add(new JLabel("Update Lifecycle:"));

        statusCombo = new JComboBox<>(new String[] { "OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED" });
        statusCombo.setFont(UIConstants.FONT_BODY);
        statusCombo.setLightWeightPopupEnabled(false);
        statusActionRow.add(statusCombo);

        updateStatusBtn = UIHelper.createStyledButton("Save Status", UIConstants.PRIMARY_COLOR, Color.WHITE);
        updateStatusBtn.setEnabled(false);
        updateStatusBtn.addActionListener(e -> handleStatusUpdate());
        statusActionRow.add(updateStatusBtn);

        markResolvedBtn = UIHelper.createStyledButton("? Quick Resolve", UIConstants.SUCCESS_COLOR, Color.WHITE);
        markResolvedBtn.setEnabled(false);
        markResolvedBtn.addActionListener(e -> handleQuickResolve());
        statusActionRow.add(markResolvedBtn);

        centerPanel.add(statusActionRow);
        centerPanel.add(Box.createVerticalStrut(10));

        JLabel noteLabel = new JLabel("Add Resolution / Progress Note:");
        noteLabel.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(noteLabel);
        centerPanel.add(Box.createVerticalStrut(4));

        noteArea = new JTextArea(3, 30);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setFont(UIConstants.FONT_BODY);
        JScrollPane noteScroll = new JScrollPane(noteArea);
        noteScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(noteScroll);
        centerPanel.add(Box.createVerticalStrut(6));

        addNoteBtn = new JButton("Post Note");
        addNoteBtn.setFont(UIConstants.FONT_BODY);
        addNoteBtn.setEnabled(false);
        addNoteBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addNoteBtn.addActionListener(e -> handleAddNote());
        centerPanel.add(addNoteBtn);
        centerPanel.add(Box.createVerticalStrut(12));

        JLabel historyLabel = new JLabel("Audit / Notes History:");
        historyLabel.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        historyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(historyLabel);
        centerPanel.add(Box.createVerticalStrut(4));

        notesHistoryArea = new JTextArea(5, 30);
        notesHistoryArea.setEditable(false);
        notesHistoryArea.setFont(UIConstants.FONT_SMALL);
        notesHistoryArea.setBackground(new Color(249, 250, 251));
        JScrollPane historyScroll = new JScrollPane(notesHistoryArea);
        historyScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(historyScroll);

        rightPanel.add(centerPanel, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);
    }

    public void loadAgentTickets() {
        tableModel.setRowCount(0);
        if (currentAgent == null) {
            currentAgent = agentService.getAgentByUserId(currentUser.getUserId()).orElse(null);
        }

        List<Ticket> tickets;
        if (currentAgent != null) {
            tickets = ticketService.getTicketsForAgent(currentAgent.getAgentId());
        } else {
            tickets = ticketService.getAllTickets().stream()
                    .filter(t -> t.getAgentId() != null)
                    .toList();
        }

        for (Ticket t : tickets) {
            tableModel.addRow(new Object[] {
                    t.getTicketId(),
                    t.getTitle(),
                    t.getCategory(),
                    t.getPriority(),
                    t.getStatus(),
                    t.getCreatedAt() != null ? dateFormat.format(t.getCreatedAt()) : "-"
            });
        }
    }

    private void onTicketSelected() {
        int row = ticketTable.getSelectedRow();
        if (row >= 0) {
            selectedTicketId = (Integer) tableModel.getValueAt(row, 0);
            Ticket ticket = ticketService.getTicketById(selectedTicketId);

            detailTitleLabel.setText("#" + ticket.getTicketId() + " - " + ticket.getTitle());
            descriptionArea.setText(ticket.getDescription());
            statusCombo.setSelectedItem(ticket.getStatus());

            updateStatusBtn.setEnabled(true);
            markResolvedBtn.setEnabled(!"RESOLVED".equalsIgnoreCase(ticket.getStatus()));
            addNoteBtn.setEnabled(true);

            loadNotesHistory(selectedTicketId);
        } else {
            selectedTicketId = null;
            detailTitleLabel.setText("Select a ticket to review details");
            descriptionArea.setText("");
            notesHistoryArea.setText("");
            updateStatusBtn.setEnabled(false);
            markResolvedBtn.setEnabled(false);
            addNoteBtn.setEnabled(false);
        }
    }

    private void loadNotesHistory(int ticketId) {
        List<TicketNote> notes = ticketService.getNotesForTicket(ticketId);
        if (notes.isEmpty()) {
            notesHistoryArea.setText("No resolution notes logged yet.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (TicketNote n : notes) {
                String time = n.getCreatedAt() != null ? dateFormat.format(n.getCreatedAt()) : "";
                String author = n.getAgentId() != null ? "Agent #" + n.getAgentId() : "System";
                sb.append("[").append(time).append("] ").append(author).append(":\n")
                        .append(n.getNote()).append("\n\n");
            }
            notesHistoryArea.setText(sb.toString());
            notesHistoryArea.setCaretPosition(0);
        }
    }

    private void handleStatusUpdate() {
        if (selectedTicketId == null)
            return;
        String newStatus = (String) statusCombo.getSelectedItem();
        try {
            ticketService.updateTicketStatus(selectedTicketId, newStatus);
            String noteText = noteArea.getText().trim();
            if (!noteText.isEmpty()) {
                Integer agentId = currentAgent != null ? currentAgent.getAgentId() : null;
                ticketService.addTicketNote(selectedTicketId, agentId, "[Status: " + newStatus + "] " + noteText);
                noteArea.setText("");
            }
            JOptionPane.showMessageDialog(this,
                    "Ticket #" + selectedTicketId + " status updated to " + newStatus,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            loadAgentTickets();
            onTicketSelected();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating status: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleQuickResolve() {
        if (selectedTicketId == null)
            return;
        String note = JOptionPane.showInputDialog(this,
                "Enter resolution summary notes for Ticket #" + selectedTicketId + ":",
                "Resolve Ticket",
                JOptionPane.PLAIN_MESSAGE);

        if (note != null) {
            try {
                ticketService.updateTicketStatus(selectedTicketId, "RESOLVED");
                Integer agentId = currentAgent != null ? currentAgent.getAgentId() : null;
                String finalNote = note.trim().isEmpty() ? "Marked as resolved." : note.trim();
                ticketService.addTicketNote(selectedTicketId, agentId, "RESOLVED: " + finalNote);

                JOptionPane.showMessageDialog(this,
                        "Ticket #" + selectedTicketId + " marked as RESOLVED!",
                        "Ticket Resolved",
                        JOptionPane.INFORMATION_MESSAGE);
                loadAgentTickets();
                onTicketSelected();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to resolve: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleAddNote() {
        if (selectedTicketId == null)
            return;
        String noteText = noteArea.getText().trim();
        if (noteText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Note cannot be empty.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Integer agentId = currentAgent != null ? currentAgent.getAgentId() : null;
            ticketService.addTicketNote(selectedTicketId, agentId, noteText);
            noteArea.setText("");
            loadNotesHistory(selectedTicketId);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save note: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
