package com.sakshi.bingetogetherbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // This is the initial handshake URL your React app connects to
        registry.addEndpoint("/ws-binge")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176")
                .withSockJS(); // Fallback option if a browser doesn't support raw WebSockets
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Outbound paths: Messages starting with /topic are broadcast out to room members
        registry.enableSimpleBroker("/topic");

        // Inbound paths: Messages sent from frontend to backend start with /app
        registry.setApplicationDestinationPrefixes("/app");
    }
}