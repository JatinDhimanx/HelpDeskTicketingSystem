package helpdesk.models;

import java.sql.Timestamp;

/**
 * Represents a support ticket in the help desk system.
 */
public class Ticket {

    private int       ticketId;
    private int       userId;
    private Integer   agentId;      // nullable ? no agent assigned yet
    private String    title;
    private String    description;
    private String    category;
    private String    priority;
    private String    status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp resolvedAt;   // nullable
    private String    agentName;    // joined display name (e.g. "Agent 1")
    private String    userName;     // joined display name (e.g. "User 1")

    /** No-arg constructor */
    public Ticket() {}

    /** Full constructor */
    public Ticket(int ticketId, int userId, Integer agentId,
                  String title, String description,
                  String category, String priority, String status,
                  Timestamp createdAt, Timestamp updatedAt, Timestamp resolvedAt) {
        this.ticketId    = ticketId;
        this.userId      = userId;
        this.agentId     = agentId;
        this.title       = title;
        this.description = description;
        this.category    = category;
        this.priority    = priority;
        this.status      = status;
        this.createdAt   = createdAt;
        this.updatedAt   = updatedAt;
        this.resolvedAt  = resolvedAt;
    }

    // ?? Getters & Setters ??????????????????????????????????????????????????????

    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getAgentId() { return agentId; }
    public void setAgentId(Integer agentId) { this.agentId = agentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Timestamp getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    @Override
    public String toString() {
        return "Ticket{ticketId=" + ticketId +
               ", userId=" + userId +
               ", agentId=" + agentId +
               ", title='" + title + "'" +
               ", category='" + category + "'" +
               ", priority='" + priority + "'" +
               ", status='" + status + "'" +
               ", createdAt=" + createdAt + "}";
    }
}
