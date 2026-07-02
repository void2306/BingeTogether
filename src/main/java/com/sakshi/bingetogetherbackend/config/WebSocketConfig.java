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
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Destination prefix for outgoing messages from server to clients
        config.enableSimpleBroker("/topic");

        // Destination prefix for inbound messages traveling from clients to @MessageMapping controllers
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 🚀 Specifying explicit origin instead of wildcard pattern to force SockJS validation bypass
        registry.addEndpoint("/ws-binge")
                .setAllowedOrigins("https://bingetogether.vercel.app")
                .withSockJS();

        registry.addEndpoint("/ws")
                .setAllowedOrigins("https://bingetogether.vercel.app");
    }
}