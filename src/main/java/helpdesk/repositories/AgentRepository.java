package helpdesk.repositories;

import helpdesk.database.DBConnection;
import helpdesk.models.Agent;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Data-access object for the agents table.
 * Joins with users table to populate name and email fields.
 */
public class AgentRepository {

    private static final Logger logger = LoggerFactory.getLogger(AgentRepository.class);

    private Agent map(ResultSet rs) throws SQLException {
        Agent agent = new Agent();
        agent.setAgentId(rs.getInt("agent_id"));
        agent.setUserId(rs.getInt("user_id"));
        agent.setAvailable(rs.getBoolean("available"));
        try { agent.setName(rs.getString("name")); } catch (SQLException ignored) {}
        try { agent.setEmail(rs.getString("email")); } catch (SQLException ignored) {}
        return agent;
    }

    private static final String SELECT_WITH_USER =
            "SELECT a.agent_id, a.user_id, a.available, u.name, u.email " +
            "FROM agents a JOIN users u ON a.user_id = u.user_id";

    public List<Agent> findAll() {
        List<Agent> list = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_WITH_USER + " ORDER BY a.agent_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            logger.error("findAll agents failed: {}", e.getMessage());
        }
        return list;
    }

    public Optional<Agent> findById(int agentId) {
        String sql = SELECT_WITH_USER + " WHERE a.agent_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.error("findById agent failed for id={}: {}", agentId, e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Agent> findByUserId(int userId) {
        String sql = SELECT_WITH_USER + " WHERE a.user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.error("findByUserId agent failed for userId={}: {}", userId, e.getMessage());
        }
        return Optional.empty();
    }

    public void save(Agent agent) {
        String sql = "INSERT INTO agents (user_id, available) VALUES (?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, agent.getUserId());
            ps.setBoolean(2, agent.isAvailable());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) agent.setAgentId(keys.getInt(1));
            }
            logger.info("Agent saved: id={}", agent.getAgentId());
        } catch (SQLException e) {
            logger.error("save(Agent) failed: {}", e.getMessage());
        }
    }

    /**
     * Atomically creates a User account with role 'AGENT' and registers them in the agents table.
     */
    public Agent createAgentWithUser(String name, String email, String plainPassword) throws SQLException {
        String hash = BCrypt.hashpw(plainPassword != null ? plainPassword : "admin123", BCrypt.gensalt(10));
        
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insert User
                int userId = -1;
                String insertUserSql = "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, 'AGENT')";
                try (PreparedStatement psUser = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                    psUser.setString(1, name);
                    psUser.setString(2, email);
                    psUser.setString(3, hash);
                    psUser.executeUpdate();
                    try (ResultSet keys = psUser.getGeneratedKeys()) {
                        if (keys.next()) userId = keys.getInt(1);
                    }
                }

                if (userId <= 0) {
                    throw new SQLException("Failed to retrieve generated user_id for new agent.");
                }

                // 2. Insert Agent
                int agentId = -1;
                String insertAgentSql = "INSERT INTO agents (user_id, available) VALUES (?, TRUE)";
                try (PreparedStatement psAgent = conn.prepareStatement(insertAgentSql, Statement.RETURN_GENERATED_KEYS)) {
                    psAgent.setInt(1, userId);
                    psAgent.executeUpdate();
                    try (ResultSet keys = psAgent.getGeneratedKeys()) {
                        if (keys.next()) agentId = keys.getInt(1);
                    }
                }

                conn.commit();
                logger.info("Created new agent: id={} name={} email={}", agentId, name, email);
                return new Agent(agentId, userId, name, email, true);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Removes an agent and their associated user account.
     * Sets any active assigned tickets back to OPEN / unassigned.
     */
    public boolean delete(int agentId) {
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                int userId = -1;
                try (PreparedStatement psGet = conn.prepareStatement("SELECT user_id FROM agents WHERE agent_id = ?")) {
                    psGet.setInt(1, agentId);
                    try (ResultSet rs = psGet.executeQuery()) {
                        if (rs.next()) userId = rs.getInt(1);
                    }
                }

                if (userId > 0) {
                    // Reset assigned tickets back to unassigned OPEN
                    try (PreparedStatement psTickets = conn.prepareStatement(
                            "UPDATE tickets SET agent_id = NULL, status = 'OPEN' WHERE agent_id = ? AND status != 'RESOLVED'")) {
                        psTickets.setInt(1, agentId);
                        psTickets.executeUpdate();
                    }

                    // Delete from agents
                    try (PreparedStatement psDelAgent = conn.prepareStatement("DELETE FROM agents WHERE agent_id = ?")) {
                        psDelAgent.setInt(1, agentId);
                        psDelAgent.executeUpdate();
                    }

                    // Delete from users
                    try (PreparedStatement psDelUser = conn.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
                        psDelUser.setInt(1, userId);
                        psDelUser.executeUpdate();
                    }

                    conn.commit();
                    logger.info("Deleted agent id={} and userId={}", agentId, userId);
                    return true;
                }
            } catch (SQLException e) {
                conn.rollback();
                logger.error("delete agent failed for id={}: {}", agentId, e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("delete agent connection failure: {}", e.getMessage());
        }
        return false;
    }

    public void update(Agent agent) {
        String sql = "UPDATE agents SET user_id=?, available=? WHERE agent_id=?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, agent.getUserId());
            ps.setBoolean(2, agent.isAvailable());
            ps.setInt(3, agent.getAgentId());
            ps.executeUpdate();
            logger.info("Agent updated: id={}", agent.getAgentId());
        } catch (SQLException e) {
            logger.error("update(Agent) failed: {}", e.getMessage());
        }
    }

    public void setAvailability(int agentId, boolean available) {
        String sql = "UPDATE agents SET available=? WHERE agent_id=?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, available);
            ps.setInt(2, agentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("setAvailability failed for agentId={}: {}", agentId, e.getMessage());
        }
    }

    public List<Agent> getAvailableAgents() {
        List<Agent> list = new ArrayList<>();
        String sql = SELECT_WITH_USER + " WHERE a.available = TRUE ORDER BY a.agent_id";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            logger.error("getAvailableAgents failed: {}", e.getMessage());
        }
        return list;
    }

    /**
     * Fast single-query active workload counter for a specific agent.
     */
    public int getAgentWorkload(int agentId) {
        String sql = "SELECT COUNT(*) FROM tickets WHERE agent_id = ? AND status != 'RESOLVED'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("getAgentWorkload failed for agentId={}: {}", agentId, e.getMessage());
        }
        return 0;
    }

    /**
     * Fast single-round-trip bulk workload counter for all agents.
     */
    public Map<Integer, Integer> getAllAgentWorkloads() {
        Map<Integer, Integer> workloads = new HashMap<>();
        String sql = "SELECT agent_id, COUNT(*) as cnt FROM tickets WHERE agent_id IS NOT NULL AND status != 'RESOLVED' GROUP BY agent_id";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                workloads.put(rs.getInt("agent_id"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            logger.error("getAllAgentWorkloads failed: {}", e.getMessage());
        }
        return workloads;
    }
}
