package helpdesk.repositories;

import helpdesk.database.DBConnection;
import helpdesk.models.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for the tickets table.
 */
public class TicketRepository {

    private static final Logger logger = LoggerFactory.getLogger(TicketRepository.class);

    private Ticket map(ResultSet rs) throws SQLException {
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
        return t;
    }

    private static final String BASE_SELECT = 
            "SELECT t.*, u_agent.name AS agent_name, u_user.name AS user_name " +
            "FROM tickets t " +
            "LEFT JOIN agents a ON t.agent_id = a.agent_id " +
            "LEFT JOIN users u_agent ON a.user_id = u_agent.user_id " +
            "LEFT JOIN users u_user ON t.user_id = u_user.user_id";

    public Optional<Ticket> findById(int ticketId) {
        String sql = BASE_SELECT + " WHERE t.ticket_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.error("findById ticket failed for id={}: {}", ticketId, e.getMessage());
        }
        return Optional.empty();
    }

    public List<Ticket> findAll() {
        List<Ticket> list = new ArrayList<>();
        String sql = BASE_SELECT + " ORDER BY t.created_at DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            logger.error("findAll tickets failed: {}", e.getMessage());
        }
        return list;
    }

    public List<Ticket> findByUserId(int userId) {
        List<Ticket> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE t.user_id = ? ORDER BY t.created_at DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            logger.error("findByUserId tickets failed for userId={}: {}", userId, e.getMessage());
        }
        return list;
    }

    public List<Ticket> findByAgentId(int agentId) {
        List<Ticket> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE t.agent_id = ? ORDER BY t.created_at DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            logger.error("findByAgentId tickets failed for agentId={}: {}", agentId, e.getMessage());
        }
        return list;
    }

    /** Inserts a new ticket and sets the generated ticket_id on the object. */
    public void save(Ticket ticket) {
        String sql = "INSERT INTO tickets (user_id, agent_id, title, description, category, priority, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ticket.getUserId());
            if (ticket.getAgentId() == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, ticket.getAgentId());
            ps.setString(3, ticket.getTitle());
            ps.setString(4, ticket.getDescription());
            ps.setString(5, ticket.getCategory());
            ps.setString(6, ticket.getPriority());
            ps.setString(7, ticket.getStatus() != null ? ticket.getStatus() : "OPEN");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) ticket.setTicketId(keys.getInt(1));
            }
            logger.info("Ticket saved: id={}", ticket.getTicketId());
        } catch (SQLException e) {
            logger.error("save(Ticket) failed: {}", e.getMessage());
        }
    }

    public void update(Ticket ticket) {
        String sql = "UPDATE tickets SET user_id=?, agent_id=?, title=?, description=?, " +
                     "category=?, priority=?, status=?, resolved_at=? WHERE ticket_id=?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticket.getUserId());
            if (ticket.getAgentId() == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, ticket.getAgentId());
            ps.setString(3, ticket.getTitle());
            ps.setString(4, ticket.getDescription());
            ps.setString(5, ticket.getCategory());
            ps.setString(6, ticket.getPriority());
            ps.setString(7, ticket.getStatus());
            ps.setTimestamp(8, ticket.getResolvedAt());
            ps.setInt(9, ticket.getTicketId());
            ps.executeUpdate();
            logger.info("Ticket updated: id={}", ticket.getTicketId());
        } catch (SQLException e) {
            logger.error("update(Ticket) failed: {}", e.getMessage());
        }
    }

    public void updateStatus(int ticketId, String status) {
        String sql = "UPDATE tickets SET status=? WHERE ticket_id=?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, ticketId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("updateStatus failed for ticketId={}: {}", ticketId, e.getMessage());
        }
    }

    public void updateAgent(int ticketId, Integer agentId) {
        String sql = "UPDATE tickets SET agent_id=? WHERE ticket_id=?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (agentId == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, agentId);
            ps.setInt(2, ticketId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("updateAgent failed for ticketId={}: {}", ticketId, e.getMessage());
        }
    }

    public void delete(int ticketId) {
        String sql = "DELETE FROM tickets WHERE ticket_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            ps.executeUpdate();
            logger.info("Ticket deleted: id={}", ticketId);
        } catch (SQLException e) {
            logger.error("delete(Ticket) failed for id={}: {}", ticketId, e.getMessage());
        }
    }

    /**
     * Flexible filter query. Any null/empty parameter is treated as "no filter".
     * 'search' matches against title or description (LIKE).
     */
    public List<Ticket> findByFilters(String category, String priority, String status, String search) {
        List<Ticket> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (category != null && !category.isEmpty()) {
            sql.append(" AND t.category = ?");
            params.add(category);
        }
        if (priority != null && !priority.isEmpty()) {
            sql.append(" AND t.priority = ?");
            params.add(priority);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND t.status = ?");
            params.add(status);
        }
        if (search != null && !search.isEmpty()) {
            sql.append(" AND (t.title LIKE ? OR t.description LIKE ?)");
            String like = "%" + search + "%";
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY t.created_at DESC");

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            logger.error("findByFilters failed: {}", e.getMessage());
        }
        return list;
    }

    public void updatePriority(int ticketId, String newPriority) {
        String sql = "UPDATE tickets SET priority=? WHERE ticket_id=?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPriority);
            ps.setInt(2, ticketId);
            ps.executeUpdate();
            logger.info("Ticket #{} priority updated to {}", ticketId, newPriority);
        } catch (SQLException e) {
            logger.error("updatePriority failed for ticketId={}: {}", ticketId, e.getMessage());
        }
    }

    public void markResolved(int ticketId) {
        String sql = "UPDATE tickets SET status='RESOLVED', resolved_at=CURRENT_TIMESTAMP WHERE ticket_id=?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            ps.executeUpdate();
            logger.info("Ticket #{} marked as RESOLVED", ticketId);
        } catch (SQLException e) {
            logger.error("markResolved failed for ticketId={}: {}", ticketId, e.getMessage());
        }
    }

    public void saveNote(int ticketId, Integer agentId, String note) {
        String sql = "INSERT INTO ticket_notes (ticket_id, agent_id, note) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            if (agentId == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, agentId);
            ps.setString(3, note);
            ps.executeUpdate();
            logger.info("Note added for ticket #{}", ticketId);
        } catch (SQLException e) {
            logger.error("saveNote failed for ticketId={}: {}", ticketId, e.getMessage());
        }
    }

    public List<helpdesk.models.TicketNote> getNotesByTicketId(int ticketId) {
        List<helpdesk.models.TicketNote> notes = new ArrayList<>();
        String sql = "SELECT * FROM ticket_notes WHERE ticket_id = ? ORDER BY created_at ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int agentId = rs.getInt("agent_id");
                    helpdesk.models.TicketNote n = new helpdesk.models.TicketNote(
                            rs.getInt("note_id"),
                            rs.getInt("ticket_id"),
                            rs.wasNull() ? null : agentId,
                            rs.getString("note"),
                            rs.getTimestamp("created_at")
                    );
                    notes.add(n);
                }
            }
        } catch (SQLException e) {
            logger.error("getNotesByTicketId failed: {}", e.getMessage());
        }
        return notes;
    }

    public List<Ticket> findUnresolved() {
        List<Ticket> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE t.status != 'RESOLVED' ORDER BY t.created_at ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            logger.error("findUnresolved failed: {}", e.getMessage());
        }
        return list;
    }
}