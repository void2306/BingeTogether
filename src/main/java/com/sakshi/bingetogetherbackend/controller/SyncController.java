package com.sakshi.bingetogetherbackend.controller;

import com.sakshi.bingetogetherbackend.dto.SyncMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;


@Controller
public class SyncController {

    /**
     * Intercepts messages sent to: /app/room/{roomCode}/sync
     * Broadcasts back out to all members listening on: /topic/room/{roomCode}/stream
     */
    @MessageMapping("/room/{roomCode}/sync")
    @SendTo("/topic/room/{roomCode}/stream")
    public SyncMessage handleVideoSync(@DestinationVariable String roomCode, SyncMessage message) {
        System.out.println("[WEBSOCKET] Action " + message.getAction() +
                " received from " + message.getSender() +
                " in room: " + roomCode + " at " + message.getTargetTime() + "s");

        // Returns the object framework straight to all subscribed room instances instantly
        return message;
    }
}