package com.amit.collabdoc.security;

import com.amit.collabdoc.model.User;
import com.amit.collabdoc.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList; // Using empty list for authorities for now

/**
 * This class implements Spring Security's UserDetailsService.
 * Its job is to load a user's details from the database given a username.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * This method is called by Spring Security when it needs to authenticate a user.
     *
     * @param username The username (or email) provided by the user.
     * @return a UserDetails object that Spring Security can use for authentication.
     * @throws UsernameNotFoundException if the user is not found in the database.
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // We will allow users to log in with either their username or email.
        // We find the user in our database.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // We convert our User entity into Spring Security's UserDetails object.
        // For this simple app, we aren't using roles, so we pass an empty list of authorities.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                new ArrayList<>() // Empty list for authorities (e.g., "ROLE_USER", "ROLE_ADMIN")
        );
    }
}
