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
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 🔑 Since allowedOrigins matches credentials policy, this pairs perfectly with the CorsFilter!
        registry.addEndpoint("/ws-binge")
                .setAllowedOrigins("https://bingetogether.vercel.app")
                .withSockJS();

        registry.addEndpoint("/ws")
                .setAllowedOrigins("https://bingetogether.vercel.app");
    }
}