package helpdesk.exceptions;

/**
 * Thrown when a ticket cannot be found by the given identifier.
 */
public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(String message) {
        super(message);
    }

    public TicketNotFoundException(int ticketId) {
        super("Ticket not found with ID: " + ticketId);
    }
}
