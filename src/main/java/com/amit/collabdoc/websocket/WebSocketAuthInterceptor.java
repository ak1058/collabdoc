package com.amit.collabdoc.websocket;

import com.amit.collabdoc.security.JwtTokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * This method is called before a message is sent.
     * We use it to intercept the initial "CONNECT" command.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // StompHeaderAccessor allows us to read the STOMP headers
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // We only care about the "CONNECT" command
        assert accessor != null;
        if (StompCommand.CONNECT.equals(accessor.getCommand())){
            // The client should send the token in a header named "Authorization"
            // Note: STOMP headers can be multi-valued, so we get the first one.

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String token = null;
            if (authHeader!=null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            if (token != null && jwtTokenProvider.validateToken(token)) {
                // If the token is valid, we extract the user's authentication
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                // We set the user in the SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // We also set the authenticated user on the WebSocket session itself
                accessor.setUser(authentication);
            }else {
                // If the token is invalid or missing, we could throw an exception
                // here to reject the connection.
                // For this project, we'll just let them connect anonymously,
                // but our controller logic will block them later.
                // A stricter approach would be:
                // throw new MessagingException("Invalid or missing token");
                throw new MessagingException("Invalid or missing token");
            }
        }return message;
    }
}
