
package helpdesk.services;

import helpdesk.database.DBConnection;
import helpdesk.models.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Service providing database aggregate metrics and report queries (Task 10).
 * Highly optimized with grouped queries to minimize network roundtrips.
 */
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    public static class KPIStats {
        public int total;
        public int open;
        public int assigned;
        public int inProgress;
        public int resolved;
        public double avgResolutionHours;
    }

    /**
     * Consolidates status counts, total count, and average resolution time in a SINGLE database roundtrip.
     * Dramatically accelerates dashboard metrics over remote TLS connections.
     */
    public KPIStats getKPIStats() {
        KPIStats stats = new KPIStats();
        String sql = "SELECT COUNT(*) AS total, " +
                     "SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END) AS open_cnt, " +
                     "SUM(CASE WHEN status = 'ASSIGNED' THEN 1 ELSE 0 END) AS assigned_cnt, " +
                     "SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress_cnt, " +
                     "SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) AS resolved_cnt, " +
                     "AVG(CASE WHEN status = 'RESOLVED' AND resolved_at IS NOT NULL " +
                     "         THEN TIMESTAMPDIFF(MINUTE, created_at, resolved_at) / 60.0 " +
                     "         ELSE NULL END) AS avg_hours " +
                     "FROM tickets";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.total = rs.getInt("total");
                stats.open = rs.getInt("open_cnt");
                stats.assigned = rs.getInt("assigned_cnt");
                stats.inProgress = rs.getInt("in_progress_cnt");
                stats.resolved = rs.getInt("resolved_cnt");
                double avg = rs.getDouble("avg_hours");
                stats.avgResolutionHours = rs.wasNull() ? 0.0 : Math.round(avg * 10.0) / 10.0;
            }
        } catch (SQLException e) {
            logger.error("getKPIStats failed: {}", e.getMessage());
        }
        return stats;
    }

    public Map<String, Integer> getStatusCounts() {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT status, COUNT(*) as cnt FROM tickets GROUP BY status";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("status").toUpperCase(), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            logger.error("getStatusCounts failed: {}", e.getMessage());
        }
        return map;
    }

    public int getTotalTicketCount() {
        return getCount("SELECT COUNT(*) FROM tickets");
    }

    public int getTicketCountByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM tickets WHERE status = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("getTicketCountByStatus failed for {}: {}", status, e.getMessage());
        }
        return 0;
    }

    public Map<String, Integer> getTicketsByCategory() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT category, COUNT(*) as cnt FROM tickets GROUP BY category ORDER BY cnt DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("category"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            logger.error("getTicketsByCategory failed: {}", e.getMessage());
        }
        return map;
    }

    public Map<String, Integer> getTicketsByPriority() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT priority, COUNT(*) as cnt FROM tickets GROUP BY priority " +
                     "ORDER BY FIELD(priority, 'Critical', 'High', 'Medium', 'Low')";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("priority"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            logger.error("getTicketsByPriority failed: {}", e.getMessage());
        }
        return map;
    }

    public List<AgentResolvedStats> getAgentResolvedCounts() {
        List<AgentResolvedStats> list = new ArrayList<>();
        String sql = "SELECT a.agent_id, u.name, u.email, COUNT(t.ticket_id) AS resolved_count " +
                     "FROM agents a " +
                     "JOIN users u ON a.user_id = u.user_id " +
                     "LEFT JOIN tickets t ON a.agent_id = t.agent_id AND t.status = 'RESOLVED' " +
                     "GROUP BY a.agent_id, u.name, u.email " +
                     "ORDER BY resolved_count DESC, u.name ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new AgentResolvedStats(
                        rs.getInt("agent_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getInt("resolved_count")
                ));
            }
        } catch (SQLException e) {
            logger.error("getAgentResolvedCounts failed: {}", e.getMessage());
        }
        return list;
    }

    public List<Ticket> getPendingTickets() {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT t.*, u_agent.name AS agent_name, u_user.name AS user_name " +
                     "FROM tickets t " +
                     "LEFT JOIN agents a ON t.agent_id = a.agent_id " +
                     "LEFT JOIN users u_agent ON a.user_id = u_agent.user_id " +
                     "LEFT JOIN users u_user ON t.user_id = u_user.user_id " +
                     "WHERE t.status != 'RESOLVED' " +
                     "ORDER BY FIELD(t.priority, 'Critical', 'High', 'Medium', 'Low'), t.created_at ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Ticket t = new Ticket();
                t.setTicketId(rs.getInt("ticket_id"));
                t.setUserId(rs.getInt("user_id"));
                int agentId = rs.getInt("agent_id");
                t.setAgentId(rs.wasNull() ? null : agentId);
                t.setTitle(rs.getString("title"));
                t.setDescription(rs.getString("description"));
                t.setCategory(rs.getString("category"));
                t.setPriority(rs.getString("priority"));
                t.setStatus(rs.getString("status"));
                t.setCreatedAt(rs.getTimestamp("created_at"));
                t.setUpdatedAt(rs.getTimestamp("updated_at"));
                t.setResolvedAt(rs.getTimestamp("resolved_at"));
                try {
                    t.setAgentName(rs.getString("agent_name"));
                } catch (SQLException ignored) {}
                try {
                    t.setUserName(rs.getString("user_name"));
                } catch (SQLException ignored) {}
                list.add(t);
            }
        } catch (SQLException e) {
            logger.error("getPendingTickets failed: {}", e.getMessage());
        }
        return list;
    }

    public double getAverageResolutionTimeHours() {
        String sql = "SELECT AVG(TIMESTAMPDIFF(HOUR, created_at, resolved_at)) AS avg_hours " +
                     "FROM tickets WHERE status = 'RESOLVED' AND resolved_at IS NOT NULL";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double val = rs.getDouble("avg_hours");
                return rs.wasNull() ? 0.0 : Math.round(val * 10.0) / 10.0;
            }
        } catch (SQLException e) {
            logger.error("getAverageResolutionTimeHours failed: {}", e.getMessage());
        }
        return 0.0;
    }

    private int getCount(String sql) {
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Query count failed: {}", e.getMessage());
        }
        return 0;
    }

    public static class AgentResolvedStats {
        private final int agentId;
        private final String name;
        private final String email;
        private final int resolvedCount;

        public AgentResolvedStats(int agentId, String name, String email, int resolvedCount) {
            this.agentId = agentId;
            this.name = name;
            this.email = email;
            this.resolvedCount = resolvedCount;
        }

        public int getAgentId() { return agentId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public int getResolvedCount() { return resolvedCount; }
    }
}
