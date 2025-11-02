package com.amit.collabdoc.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility class for handling JWT (JSON Web Token) operations:
 * 1. Generating a new token.
 * 2. Validating an existing token.
 * 3. Parsing the username from a token.
 * 4. Getting Authentication object from a token
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final UserDetailsService userDetailsService;

    // These values are injected from our application.yml file
    @Value("${app.jwt-secret}")
    private String jwtSecret;

    @Value("${app.jwt-expiration-ms}")
    private int jwtExpirationInMs;

    private SecretKey key;

    public JwtTokenProvider(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }


    /**
     * Generates a JWT for a successfully authenticated user.
     */
    public String generateToken(Authentication authentication) {
        // The "principal" is the user object we created in UserDetailsServiceImpl
        String username = authentication.getName();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(getSigningKey()) // Sign the token with our secret key
                .compact();
    }

    /**
     * Extracts the username from a given JWT.
     */
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Validates if a JWT is correct and not expired.
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        } catch (Exception ex) {
            logger.error("Invalid JWT signature");
        }
        return false;
    }

    /**
     * Helper method to get the signing key from the jwt-secret.
     */
    private SecretKey getSigningKey() {
        if (key == null) {
            byte[] keyBytes = Decoders.BASE64.decode(this.jwtSecret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
        return key;
    }

    /**
     * Takes a valid token and returns a Spring Security Authentication object.
     */
    public Authentication getAuthentication(String token) {
        // 1. Get the username from the token
        String username = getUsernameFromJWT(token);

        // 2. Load the user's details (including roles/authorities)
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // 3. Create an authenticated token
        // This is the object that will be stored in the SecurityContext
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
