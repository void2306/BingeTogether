package com.sakshi.bingetogetherbackend.controller;

import com.sakshi.bingetogetherbackend.dto.SyncMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class SyncController {

    /**
     * Handles live real-time synchronization updates (scrubbing, play, pause states)
     * Inbound Destination App Endpoint: /app/room/{roomCode}/sync
     * Outbound Topic Streaming Channel: /topic/room/{roomCode}/stream
     */
    @MessageMapping("/room/{roomCode}/sync")
    @SendTo("/topic/room/{roomCode}/stream")
    public SyncMessage handleVideoSync(
            @DestinationVariable String roomCode,
            SyncMessage syncMessage) {

        System.out.println("[WS-SYNC] Room: " + roomCode
                + " | Sender: " + syncMessage.getSender()
                + " | Action: " + syncMessage.getAction()
                + " | TargetTime: " + syncMessage.getTargetTime());

        // Broadcasts the payload data instantly to everyone else listening in the room party channel
        return syncMessage;
    }
}