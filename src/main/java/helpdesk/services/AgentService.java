package helpdesk.services;

import helpdesk.models.Agent;
import helpdesk.models.Ticket;
import helpdesk.repositories.AgentRepository;
import helpdesk.repositories.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service managing agent assignments, lifecycle management, and workload tracking (Task 07 / Admin Management).
 */
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);

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

    /**
     * Fast single-query active workload counter.
     */
    public int getAgentWorkload(int agentId) {
        return agentRepository.getAgentWorkload(agentId);
    }

    /**
     * Bulk workload retrieval for all agents in a single network round-trip.
     */
    public Map<Integer, Integer> getAllAgentWorkloads() {
        return agentRepository.getAllAgentWorkloads();
    }

    /**
     * Admin capability: creates a new agent account and assigns them to the agents pool.
     */
    public Agent createAgent(String name, String email, String password) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent name cannot be empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent email cannot be empty");
        }
        return agentRepository.createAgentWithUser(name.trim(), email.trim().toLowerCase(), password);
    }

    /**
     * Admin capability: removes an agent and frees any active tickets.
     */
    public boolean deleteAgent(int agentId) {
        return agentRepository.delete(agentId);
    }

    /**
     * Automatically computes the next suggested agent sequential label (e.g. "Agent 3").
     */
    public String[] suggestNextAgent() {
        List<Agent> agents = agentRepository.findAll();
        int maxNum = 0;
        Pattern p = Pattern.compile("Agent\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        
        for (Agent a : agents) {
            String name = a.getName();
            if (name != null) {
                Matcher m = p.matcher(name);
                if (m.find()) {
                    try {
                        int num = Integer.parseInt(m.group(1));
                        if (num > maxNum) maxNum = num;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        int next = maxNum > 0 ? (maxNum + 1) : (agents.size() + 1);
        String suggestedName = "Agent " + next;
        String suggestedEmail = "agent" + next + "@helpdesk.com";
        return new String[]{suggestedName, suggestedEmail};
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
