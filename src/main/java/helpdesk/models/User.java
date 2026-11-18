package helpdesk.models;

import java.sql.Timestamp;

/**
 * Represents a registered user of the help desk system.
 */
public class User extends Person {

    private int       userId;
    private String    passwordHash;
    private String    role;
    private Timestamp createdAt;

    /** No-arg constructor */
    public User() {
        super("", "");
    }

    /** Full constructor */
    public User(int userId, String name, String email, String passwordHash, String role, Timestamp createdAt) {
        super(name, email);
        this.userId       = userId;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.createdAt    = createdAt;
    }

    /** Convenience constructor (without id/timestamp) */
    public User(String name, String email, String passwordHash, String role) {
        super(name, email);
        this.passwordHash = passwordHash;
        this.role         = role;
    }

    // ?? Getters & Setters ??????????????????????????????????????????????????????

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public void setRole(String role) { this.role = role; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String getRole() { return role; }

    @Override
    public String toString() {
        return "User{userId=" + userId + ", name='" + name + "', email='" + email +
               "', role='" + role + "', createdAt=" + createdAt + "}";
    }
}
