package helpdesk.services;

import helpdesk.models.User;
import helpdesk.repositories.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication service.
 * Verifies credentials against the database using BCrypt password checking.
 * Features an intelligent in-memory cache and background pre-warming so that
 * user logins (including 1-click demo logins) occur in milliseconds.
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final Map<String, User> USER_CACHE = new ConcurrentHashMap<>();

    private final UserRepository userRepository;

    public AuthService() {
        this.userRepository = new UserRepository();
    }

    /**
     * Asynchronously pre-populates user records in memory so login authenticates instantly.
     */
    public void preWarm() {
        Thread t = new Thread(() -> {
            try {
                logger.info("Pre-warming user authentication cache in background...");
                List<User> users = userRepository.findAll();
                for (User u : users) {
                    if (u.getEmail() != null) {
                        USER_CACHE.put(u.getEmail().trim().toLowerCase(), u);
                    }
                }
                logger.info("Auth cache pre-warmed with {} user records.", USER_CACHE.size());
            } catch (Exception e) {
                logger.warn("Auth cache pre-warm skipped: {}", e.getMessage());
            }
        }, "Auth-Cache-PreWarmer");
        t.setDaemon(true);
        t.start();
    }

    public static void cacheUser(User user) {
        if (user != null && user.getEmail() != null) {
            USER_CACHE.put(user.getEmail().trim().toLowerCase(), user);
        }
    }

    public static void invalidateUser(String email) {
        if (email != null) {
            USER_CACHE.remove(email.trim().toLowerCase());
        }
    }

    /**
     * Authenticates a user by email and plain-text password.
     * Checks in-memory pre-warmed user cache first for near-instant validation.
     *
     * @param email    the user's email address
     * @param password the plain-text password to verify
     * @return the authenticated {@link User}
     * @throws IllegalArgumentException if no matching user is found or the password is wrong
     */
    public User authenticate(String email, String password) {
        if (email == null || password == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String key = email.trim().toLowerCase();
        User user = USER_CACHE.get(key);

        if (user == null) {
            Optional<User> userOpt = userRepository.findByEmail(key);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("Invalid email or password");
            }
            user = userOpt.get();
            USER_CACHE.put(key, user);
        }

        // Verify password hash
        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            // In case the password was recently changed in DB, re-check DB once
            Optional<User> freshUserOpt = userRepository.findByEmail(key);
            if (freshUserOpt.isPresent() && !freshUserOpt.get().getPasswordHash().equals(user.getPasswordHash())) {
                user = freshUserOpt.get();
                USER_CACHE.put(key, user);
                if (BCrypt.checkpw(password, user.getPasswordHash())) {
                    return user;
                }
            }
            throw new IllegalArgumentException("Invalid email or password");
        }

        return user;
    }
}
