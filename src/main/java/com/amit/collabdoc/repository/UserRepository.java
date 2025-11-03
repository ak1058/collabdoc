package com.amit.collabdoc.repository;

import com.amit.collabdoc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the User entity.
 * This interface handles all database operations (e.g., find, save, delete)
 * for User objects.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their username.
     * Spring Data JPA automatically implements this method based on its name.
     *
     * @param username The username to search for.
     * @return An Optional containing the User if found, or empty if not.
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by their username.
     * Spring Data JPA automatically implements this method based on its name.
     *
     * @param email The username to search for.
     * @return An Optional containing the User if found, or empty if not.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given username.
     *
     * @param username The username to check.
     * @return true if a user with this username exists, false otherwise.
     */
    Boolean existsByUsername(String username);

    /**
     * Checks if a user exists with the given email.
     *
     * @param email The email to check.
     * @return true if a user with this email exists, false otherwise.
     */
    Boolean existsByEmail(String email);

}
