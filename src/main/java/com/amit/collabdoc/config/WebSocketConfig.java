package com.amit.collabdoc.config;

import com.amit.collabdoc.websocket.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker  // This enables the WebSocket server
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // This is the HTTP endpoint that the client will connect
        // to "upgrade" to a WebSocket connection.
        registry.addEndpoint("/ws")
                //allow frontend to connect
                .setAllowedOrigins("http://localhost:5173")
                .withSockJS();   // Add SockJS fallback for older browsers
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // These are the "topics" clients will subscribe to.
        // e.g., /topic/document/123
        registry.enableSimpleBroker("/topic");

        // This is the prefix for messages sent *from* the client *to* the server.
        // e.g., /app/document/123
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * This method registers our custom interceptor to check the JWT.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
