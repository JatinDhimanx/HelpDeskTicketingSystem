package helpdesk.services;

import helpdesk.models.User;
import helpdesk.repositories.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

/**
 * Authentication service.
 * Verifies credentials against the database using BCrypt password checking.
 */
public class AuthService {

    private final UserRepository userRepository;

    public AuthService() {
        this.userRepository = new UserRepository();
    }

    /**
     * Authenticates a user by email and plain-text password.
     *
     * @param email    the user's email address
     * @param password the plain-text password to verify
     * @return the authenticated {@link User}
     * @throws IllegalArgumentException if no matching user is found or the password is wrong
     */
    public User authenticate(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        User user = userOpt.get();

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return user;
    }
}
