package helpdesk.services;

import helpdesk.models.Agent;
import helpdesk.models.Ticket;
import helpdesk.repositories.AgentRepository;
import helpdesk.repositories.TicketRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service managing agent assignments, workload tracking and availability (Task 07).
 */
public class AgentService {

    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;

    public AgentService() {
        this.agentRepository = new AgentRepository();
        this.ticketRepository = new TicketRepository();
    }

    public AgentService(AgentRepository agentRepo, TicketRepository ticketRepo) {
        this.agentRepository = agentRepo;
        this.ticketRepository = ticketRepo;
    }

    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    public List<Agent> getAvailableAgents() {
        return agentRepository.getAvailableAgents();
    }

    public Optional<Agent> getAgentById(int agentId) {
        return agentRepository.findById(agentId);
    }

    public Optional<Agent> getAgentByUserId(int userId) {
        return agentRepository.findByUserId(userId);
    }

    public void setAgentAvailability(int agentId, boolean available) {
        agentRepository.setAvailability(agentId, available);
    }

    public int getAgentWorkload(int agentId) {
        List<Ticket> tickets = ticketRepository.findByAgentId(agentId);
        int activeCount = 0;
        for (Ticket t : tickets) {
            if (!"RESOLVED".equalsIgnoreCase(t.getStatus())) {
                activeCount++;
            }
        }
        return activeCount;
    }

    /**
     * Polymorphic assignment implementing the Assignable interface contract (Task 03 / Task 07).
     */
    public void assignTicketToAgent(Agent agent, Ticket ticket) {
        if (agent == null) {
            throw new IllegalArgumentException("Agent cannot be null");
        }
        if (!agent.isAvailable()) {
            throw new IllegalStateException("Agent " + agent.getName() + " is currently unavailable.");
        }
        agent.assignTicket(ticket);
        ticketRepository.updateAgent(ticket.getTicketId(), agent.getAgentId());
        ticketRepository.updateStatus(ticket.getTicketId(), "ASSIGNED");
    }
}
