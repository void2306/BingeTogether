package com.sakshi.bingetogetherbackend.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component("myWebSocketHandler") // 👈 This names the bean so Spring can find it
public class MyWebSocketHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("[WS] New connection established from session ID: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Handled dynamically via STOMP broker, but required by fallback layers
        System.out.println("[WS] Message received locally: " + message.getPayload());
    }
}