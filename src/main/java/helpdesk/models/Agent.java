package helpdesk.models;

/**
 * Represents a support agent. Extends Person and can be assigned tickets.
 */
public class Agent extends Person implements Assignable {

    private int     agentId;
    private int     userId;
    private boolean available;

    /** No-arg constructor */
    public Agent() {
        super("", "");
    }

    /** Full constructor */
    public Agent(int agentId, int userId, String name, String email, boolean available) {
        super(name, email);
        this.agentId   = agentId;
        this.userId    = userId;
        this.available = available;
    }

    /** Convenience constructor (without agentId) */
    public Agent(int userId, String name, String email, boolean available) {
        super(name, email);
        this.userId    = userId;
        this.available = available;
    }

    // ?? Getters & Setters ??????????????????????????????????????????????????????

    public int getAgentId() { return agentId; }
    public void setAgentId(int agentId) { this.agentId = agentId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String getRole() { return "AGENT"; }

    /**
     * Assigns this agent to the given ticket by setting the ticket's agentId.
     */
    @Override
    public void assignTicket(Ticket ticket) {
        ticket.setAgentId(this.agentId);
    }

    @Override
    public String toString() {
        return "Agent{agentId=" + agentId + ", userId=" + userId +
               ", name='" + name + "', email='" + email +
               "', available=" + available + "}";
    }
}
