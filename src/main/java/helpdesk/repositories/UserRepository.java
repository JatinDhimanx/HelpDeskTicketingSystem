package helpdesk.repositories;

import helpdesk.database.DBConnection;
import helpdesk.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for the users table.
 * All methods use PreparedStatement and try-with-resources.
 */
public class UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    // ?? Map ResultSet row ? User ???????????????????????????????????????????????

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getTimestamp("created_at")
        );
    }

    // ?? Queries ????????????????????????????????????????????????????????????????

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.error("findByEmail failed for '{}': {}", email, e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<User> findById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            logger.error("findById failed for id={}: {}", userId, e.getMessage());
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY user_id";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            logger.error("findAll failed: {}", e.getMessage());
        }
        return list;
    }

    /** Inserts a new user and sets the generated user_id on the object. */
    public void save(User user) {
        String sql = "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setUserId(keys.getInt(1));
            }
            logger.info("User saved: id={}", user.getUserId());
        } catch (SQLException e) {
            logger.error("save(User) failed: {}", e.getMessage());
        }
    }

    /** Updates name, email, password_hash, and role for an existing user. */
    public void update(User user) {
        String sql = "UPDATE users SET name=?, email=?, password_hash=?, role=? WHERE user_id=?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.setInt(5, user.getUserId());
            ps.executeUpdate();
            logger.info("User updated: id={}", user.getUserId());
        } catch (SQLException e) {
            logger.error("update(User) failed: {}", e.getMessage());
        }
    }

    public void delete(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            logger.info("User deleted: id={}", userId);
        } catch (SQLException e) {
            logger.error("delete(User) failed for id={}: {}", userId, e.getMessage());
        }
    }
}
