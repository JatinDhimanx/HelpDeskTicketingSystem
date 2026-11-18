package helpdesk.models;

import java.sql.Timestamp;

/**
 * Represents a note added by an agent on a ticket.
 */
public class TicketNote {

    private int       noteId;
    private int       ticketId;
    private Integer   agentId;   // nullable
    private String    note;
    private Timestamp createdAt;

    /** No-arg constructor */
    public TicketNote() {}

    /** Full constructor */
    public TicketNote(int noteId, int ticketId, Integer agentId, String note, Timestamp createdAt) {
        this.noteId    = noteId;
        this.ticketId  = ticketId;
        this.agentId   = agentId;
        this.note      = note;
        this.createdAt = createdAt;
    }

    /** Convenience constructor (without noteId) */
    public TicketNote(int ticketId, Integer agentId, String note) {
        this.ticketId = ticketId;
        this.agentId  = agentId;
        this.note     = note;
    }

    // ?? Getters & Setters ??????????????????????????????????????????????????????

    public int getNoteId() { return noteId; }
    public void setNoteId(int noteId) { this.noteId = noteId; }

    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }

    public Integer getAgentId() { return agentId; }
    public void setAgentId(Integer agentId) { this.agentId = agentId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "TicketNote{noteId=" + noteId + ", ticketId=" + ticketId +
               ", agentId=" + agentId + ", createdAt=" + createdAt + "}";
    }
}
