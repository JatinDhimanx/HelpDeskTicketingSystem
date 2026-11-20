package helpdesk.ui;

import helpdesk.models.Ticket;
import helpdesk.models.User;
import helpdesk.services.TicketService;
import helpdesk.utils.UIConstants;
import helpdesk.utils.UIHelper;

import javax.swing.*;
import java.awt.*;

/**
 * Modern ticket creation form interface (Task 05).
 */
public class CreateTicketPanel extends JPanel {

    private final User currentUser;
    private final TicketService ticketService;
    private Runnable onTicketCreatedCallback;

    private JTextField titleField;
    private JTextArea descriptionArea;
    private JComboBox<String> categoryCombo;
    private JComboBox<String> priorityCombo;
    private JLabel statusFeedbackLabel;

    public CreateTicketPanel(User currentUser) {
        this(currentUser, null);
    }

    public CreateTicketPanel(User currentUser, Runnable onTicketCreatedCallback) {
        this.currentUser = currentUser;
        this.ticketService = new TicketService();
        this.onTicketCreatedCallback = onTicketCreatedCallback;
        initUI();
    }

    public void setOnTicketCreatedCallback(Runnable callback) {
        this.onTicketCreatedCallback = callback;
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JPanel headerPanel = new JPanel(new BorderLayout(0, 4));
        headerPanel.setOpaque(false);
        JLabel pageTitle = new JLabel("Create New Support Ticket");
        pageTitle.setFont(UIConstants.FONT_TITLE);
        pageTitle.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel pageSubtitle = new JLabel("Provide detailed information so our support agents can resolve your issue quickly.");
        pageSubtitle.setFont(UIConstants.FONT_BODY);
        pageSubtitle.setForeground(UIConstants.TEXT_SECONDARY);

        headerPanel.add(pageTitle, BorderLayout.NORTH);
        headerPanel.add(pageSubtitle, BorderLayout.SOUTH);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        add(headerPanel, BorderLayout.NORTH);

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(UIConstants.CARD_COLOR);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel titleLabel = new JLabel("Ticket Title *");
        titleLabel.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        formCard.add(titleLabel, gbc);

        gbc.gridy++;
        titleField = new JTextField();
        titleField.setFont(UIConstants.FONT_BODY);
        titleField.setPreferredSize(new Dimension(500, 36));
        formCard.add(titleField, gbc);

        gbc.gridy++;
        JPanel rowPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        rowPanel.setOpaque(false);

        JPanel catPanel = new JPanel(new BorderLayout(0, 4));
        catPanel.setOpaque(false);
        JLabel catLabel = new JLabel("Category *");
        catLabel.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        categoryCombo = new JComboBox<>(new String[]{
                "Technical", "Network", "Hardware", "Software", "Account/Login"
        });
        categoryCombo.setFont(UIConstants.FONT_BODY);
        categoryCombo.setPreferredSize(new Dimension(240, 36));
        categoryCombo.setLightWeightPopupEnabled(false);
        catPanel.add(catLabel, BorderLayout.NORTH);
        catPanel.add(categoryCombo, BorderLayout.CENTER);

        JPanel priPanel = new JPanel(new BorderLayout(0, 4));
        priPanel.setOpaque(false);
        JLabel priLabel = new JLabel("Priority *");
        priLabel.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        priorityCombo = new JComboBox<>(new String[]{
                "Low", "Medium", "High", "Critical"
        });
        priorityCombo.setFont(UIConstants.FONT_BODY);
        priorityCombo.setPreferredSize(new Dimension(240, 36));
        priorityCombo.setLightWeightPopupEnabled(false);
        priPanel.add(priLabel, BorderLayout.NORTH);
        priPanel.add(priorityCombo, BorderLayout.CENTER);

        rowPanel.add(catPanel);
        rowPanel.add(priPanel);
        formCard.add(rowPanel, gbc);

        gbc.gridy++;
        JLabel descLabel = new JLabel("Description *");
        descLabel.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        formCard.add(descLabel, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        descriptionArea = new JTextArea(6, 40);
        descriptionArea.setFont(UIConstants.FONT_BODY);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        formCard.add(descScroll, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;
        statusFeedbackLabel = new JLabel(" ");
        statusFeedbackLabel.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        formCard.add(statusFeedbackLabel, gbc);

        gbc.gridy++;
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonRow.setOpaque(false);

        JButton clearBtn = new JButton("Clear");
        clearBtn.setFont(UIConstants.FONT_BODY);
        clearBtn.setFocusPainted(false);
        clearBtn.setBackground(Color.WHITE);
        clearBtn.addActionListener(e -> clearForm());

        JButton submitBtn = UIHelper.createStyledButton("Submit Ticket", UIConstants.PRIMARY_COLOR, Color.WHITE);
        submitBtn.setPreferredSize(new Dimension(150, 38));
        submitBtn.setFont(UIConstants.FONT_BODY.deriveFont(Font.BOLD));
        submitBtn.addActionListener(e -> handleSubmit());

        buttonRow.add(clearBtn);
        buttonRow.add(submitBtn);
        formCard.add(buttonRow, gbc);

        JScrollPane scrollPane = new JScrollPane(formCard);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void handleSubmit() {
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        String category = (String) categoryCombo.getSelectedItem();
        String priority = (String) priorityCombo.getSelectedItem();

        if (title.isEmpty()) {
            showError("Ticket title cannot be empty.");
            titleField.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            showError("Please provide a description of the issue.");
            descriptionArea.requestFocus();
            return;
        }

        try {
            Ticket ticket = new Ticket();
            ticket.setUserId(currentUser.getUserId());
            ticket.setTitle(title);
            ticket.setDescription(description);
            ticket.setCategory(category);
            ticket.setPriority(priority);
            ticket.setStatus("OPEN");

            Ticket saved = ticketService.createTicket(ticket);
            showSuccess("Ticket #" + saved.getTicketId() + " submitted successfully! Status: OPEN");
            clearForm();

            if (onTicketCreatedCallback != null) {
                onTicketCreatedCallback.run();
            }
        } catch (Exception ex) {
            showError("Failed to submit ticket: " + ex.getMessage());
        }
    }

    private void clearForm() {
        titleField.setText("");
        descriptionArea.setText("");
        categoryCombo.setSelectedIndex(0);
        priorityCombo.setSelectedIndex(0);
    }

    private void showSuccess(String msg) {
        statusFeedbackLabel.setForeground(UIConstants.SUCCESS_COLOR);
        statusFeedbackLabel.setText("[OK] " + msg);
    }

    private void showError(String msg) {
        statusFeedbackLabel.setForeground(UIConstants.ERROR_COLOR);
        statusFeedbackLabel.setText("[!] " + msg);
    }
}
