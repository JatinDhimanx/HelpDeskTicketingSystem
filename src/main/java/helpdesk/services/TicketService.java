package helpdesk.services;

import helpdesk.exceptions.TicketNotFoundException;
import helpdesk.models.Ticket;
import helpdesk.models.TicketNote;
import helpdesk.repositories.TicketRepository;

import java.util.List;
import java.util.Set;

/**
 * Business service layer for ticket operations (Task 05, 06, 08).
 */
public class TicketService {

    private final TicketRepository ticketRepository;
    private static final Set<String> VALID_STATUSES = Set.of("OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED");
    private static final Set<String> VALID_CATEGORIES = Set.of("Technical", "Network", "Hardware", "Software", "Account/Login");
    private static final Set<String> VALID_PRIORITIES = Set.of("Low", "Medium", "High", "Critical");

    public TicketService() {
        this.ticketRepository = new TicketRepository();
    }

    public TicketService(TicketRepository repository) {
        this.ticketRepository = repository;
    }

    public Ticket createTicket(Ticket ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket cannot be null");
        }
        if (ticket.getTitle() == null || ticket.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket title cannot be empty");
        }
        if (ticket.getDescription() == null || ticket.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket description cannot be empty");
        }
        if (ticket.getCategory() == null || !VALID_CATEGORIES.contains(ticket.getCategory())) {
            throw new IllegalArgumentException("Invalid category: " + ticket.getCategory());
        }
        if (ticket.getPriority() == null || !VALID_PRIORITIES.contains(ticket.getPriority())) {
            ticket.setPriority("Low");
        }
        if (ticket.getStatus() == null || ticket.getStatus().trim().isEmpty()) {
            ticket.setStatus("OPEN");
        }

        ticketRepository.save(ticket);
        return ticket;
    }

    public Ticket getTicketById(int ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public List<Ticket> getTicketsForUser(int userId) {
        return ticketRepository.findByUserId(userId);
    }

    public List<Ticket> getTicketsForAgent(int agentId) {
        return ticketRepository.findByAgentId(agentId);
    }

    public void updateTicketStatus(int ticketId, String newStatus) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid ticket status: " + newStatus);
        }
        getTicketById(ticketId); // verify exists
        if ("RESOLVED".equalsIgnoreCase(newStatus)) {
            ticketRepository.markResolved(ticketId);
        } else {
            ticketRepository.updateStatus(ticketId, newStatus);
        }
    }

    public void assignAgent(int ticketId, int agentId) {
        getTicketById(ticketId);
        ticketRepository.updateAgent(ticketId, agentId);
        ticketRepository.updateStatus(ticketId, "ASSIGNED");
    }

    public List<Ticket> getFilteredTickets(String category, String priority, String status, String search) {
        return ticketRepository.findByFilters(category, priority, status, search);
    }

    public void addTicketNote(int ticketId, Integer agentId, String note) {
        if (note == null || note.trim().isEmpty()) {
            throw new IllegalArgumentException("Note cannot be empty");
        }
        ticketRepository.saveNote(ticketId, agentId, note.trim());
    }

    public List<TicketNote> getNotesForTicket(int ticketId) {
        return ticketRepository.getNotesByTicketId(ticketId);
    }

    public void updatePriority(int ticketId, String newPriority) {
        if (!VALID_PRIORITIES.contains(newPriority)) {
            throw new IllegalArgumentException("Invalid priority: " + newPriority);
        }
        ticketRepository.updatePriority(ticketId, newPriority);
    }

    public List<Ticket> getUnresolvedTickets() {
        return ticketRepository.findUnresolved();
    }
}
