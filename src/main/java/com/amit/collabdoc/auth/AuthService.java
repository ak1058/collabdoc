package com.amit.collabdoc.auth;

import com.amit.collabdoc.dto.AuthResponse;
import com.amit.collabdoc.dto.LoginRequest;
import com.amit.collabdoc.dto.RegisterRequest;
import com.amit.collabdoc.model.User;
import com.amit.collabdoc.repository.UserRepository;
import com.amit.collabdoc.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    // Constructor Injection
    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Handles user login, authentication, and JWT generation.
     */
    public AuthResponse loginUser(LoginRequest loginRequest) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Set the authentication in the security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate the JWT token
        String token = tokenProvider.generateToken(authentication);

        // Get user details to return
        // We can safely cast principal to UserDetails (or our custom User)
        // but it's cleaner to just fetch the user from the repository
        // using the username/email from the request.
        User user = userRepository.findByUsername(
                        loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found after successful login. This should not happen."));

        // Create the response DTO
        return new AuthResponse(token, user.getUsername(), user.getEmail());
    }

    /**
     * Handles new user registration.
     */
    public void registerUser(RegisterRequest registerRequest) {
        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        // Create new user's account
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        userRepository.save(user);
    }
}

