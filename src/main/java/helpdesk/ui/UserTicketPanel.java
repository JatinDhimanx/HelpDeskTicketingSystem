package helpdesk.ui;

import helpdesk.models.Ticket;
import helpdesk.models.TicketNote;
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
 * User View for tracking submitted tickets and reading resolution updates (Task
 * 08).
 */
public class UserTicketPanel extends JPanel {

    private final User currentUser;
    private final TicketService ticketService;

    private JTable ticketTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> statusFilter;
    private JLabel detailHeaderLabel;
    private JTextArea descriptionArea;
    private JTextArea resolutionNotesArea;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public UserTicketPanel(User currentUser) {
        this.currentUser = currentUser;
        this.ticketService = new TicketService();
        initUI();
        loadTickets();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JPanel titleBox = new JPanel(new BorderLayout(0, 2));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("My Support Tickets");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Track status updates and read support agent replies on your submitted tickets.");
        subtitle.setFont(UIConstants.FONT_BODY);
        subtitle.setForeground(UIConstants.TEXT_SECONDARY);
        titleBox.add(title, BorderLayout.NORTH);
        titleBox.add(subtitle, BorderLayout.SOUTH);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterRow.setOpaque(false);
        filterRow.add(new JLabel("Status:"));
        statusFilter = new JComboBox<>(new String[] { "All", "OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED" });
        statusFilter.setFont(UIConstants.FONT_BODY);
        statusFilter.addActionListener(e -> loadTickets());
        filterRow.add(statusFilter);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(UIConstants.FONT_SMALL);
        refreshBtn.addActionListener(e -> loadTickets());
        filterRow.add(refreshBtn);

        topPanel.add(titleBox, BorderLayout.WEST);
        topPanel.add(filterRow, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(520);
        splitPane.setResizeWeight(0.55);
        splitPane.setBorder(null);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(UIConstants.CARD_COLOR);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        String[] cols = { "ID", "Title", "Category", "Priority", "Status", "Submitted At" };
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

        JPanel rightPanel = new JPanel(new BorderLayout(0, 12));
        rightPanel.setBackground(UIConstants.CARD_COLOR);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        detailHeaderLabel = new JLabel("Select a ticket to view resolution information");
        detailHeaderLabel.setFont(UIConstants.FONT_HEADING);
        detailHeaderLabel.setForeground(UIConstants.PRIMARY_COLOR);
        rightPanel.add(detailHeaderLabel, BorderLayout.NORTH);

        JPanel contentStack = new JPanel(new GridLayout(2, 1, 0, 12));
        contentStack.setOpaque(false);

        descriptionArea = new JTextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(UIConstants.FONT_BODY);
        descriptionArea.setBackground(new Color(249, 250, 251));
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(BorderFactory.createTitledBorder("Ticket Description"));
        contentStack.add(descScroll);

        resolutionNotesArea = new JTextArea();
        resolutionNotesArea.setEditable(false);
        resolutionNotesArea.setLineWrap(true);
        resolutionNotesArea.setWrapStyleWord(true);
        resolutionNotesArea.setFont(UIConstants.FONT_BODY);
        resolutionNotesArea.setBackground(new Color(249, 250, 251));
        JScrollPane resScroll = new JScrollPane(resolutionNotesArea);
        resScroll.setBorder(BorderFactory.createTitledBorder("Agent Resolution & Notes History"));
        contentStack.add(resScroll);

        rightPanel.add(contentStack, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);
    }

    public void loadTickets() {
        String selectedStatus = (String) statusFilter.getSelectedItem();
        SwingWorker<List<Ticket>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Ticket> doInBackground() {
                return ticketService.getTicketsForUser(currentUser.getUserId());
            }

            @Override
            protected void done() {
                try {
                    List<Ticket> list = get();
                    tableModel.setRowCount(0);
                    for (Ticket t : list) {
                        if ("All".equalsIgnoreCase(selectedStatus) || selectedStatus.equalsIgnoreCase(t.getStatus())) {
                            tableModel.addRow(new Object[]{
                                    t.getTicketId(),
                                    t.getTitle(),
                                    t.getCategory(),
                                    t.getPriority(),
                                    t.getStatus(),
                                    t.getCreatedAt() != null ? dateFormat.format(t.getCreatedAt()) : "-"
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void onTicketSelected() {
        int row = ticketTable.getSelectedRow();
        if (row >= 0) {
            int ticketId = (Integer) tableModel.getValueAt(row, 0);
            detailHeaderLabel.setText("Loading ticket #" + ticketId + "...");
            descriptionArea.setText("Loading details...");
            resolutionNotesArea.setText("Loading updates...");

            SwingWorker<TicketDetailData, Void> worker = new SwingWorker<>() {
                @Override
                protected TicketDetailData doInBackground() {
                    Ticket t = ticketService.getTicketById(ticketId);
                    List<TicketNote> notes = ticketService.getNotesForTicket(ticketId);
                    return new TicketDetailData(t, notes);
                }

                @Override
                protected void done() {
                    try {
                        TicketDetailData data = get();
                        Ticket t = data.ticket;
                        if (t != null) {
                            String statusTag = " [" + t.getStatus() + "]";
                            String agentTag = t.getAgentName() != null && !t.getAgentName().isEmpty() 
                                    ? " | Support: " + t.getAgentName() 
                                    : "";
                            detailHeaderLabel.setText("#" + t.getTicketId() + " - " + t.getTitle() + statusTag + agentTag);
                            descriptionArea.setText(t.getDescription());

                            List<TicketNote> notes = data.notes;
                            if (notes.isEmpty()) {
                                if ("RESOLVED".equalsIgnoreCase(t.getStatus())) {
                                    resolutionNotesArea.setText("This ticket is resolved. No additional comments logged.");
                                } else {
                                    resolutionNotesArea.setText("Your ticket is currently in progress. An agent will post updates here.");
                                }
                            } else {
                                StringBuilder sb = new StringBuilder();
                                for (TicketNote n : notes) {
                                    String time = n.getCreatedAt() != null ? dateFormat.format(n.getCreatedAt()) : "";
                                    sb.append("• ").append(time).append(" Support Update:\n  ")
                                      .append(n.getNote()).append("\n\n");
                                }
                                resolutionNotesArea.setText(sb.toString());
                                resolutionNotesArea.setCaretPosition(0);
                            }
                        }
                    } catch (Exception ex) {
                        detailHeaderLabel.setText("Error loading ticket #" + ticketId);
                    }
                }
            };
            worker.execute();
        } else {
            detailHeaderLabel.setText("Select a ticket to view resolution information");
            descriptionArea.setText("");
            resolutionNotesArea.setText("");
        }
    }

    private static class TicketDetailData {
        final Ticket ticket;
        final List<TicketNote> notes;
        TicketDetailData(Ticket ticket, List<TicketNote> notes) {
            this.ticket = ticket;
            this.notes = notes;
        }
    }
}
