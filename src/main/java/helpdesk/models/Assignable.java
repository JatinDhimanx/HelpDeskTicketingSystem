package helpdesk.models;

/**
 * Contract for any entity that can be assigned a ticket.
 */
public interface Assignable {
    void assignTicket(Ticket ticket);
}
