package helpdesk.repositories;

import helpdesk.database.DBConnection;
import helpdesk.models.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        // name/email come from joined users row (may be null if no join)
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
}
