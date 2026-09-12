package com.sakshi.bingetogetherbackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class SyncController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 0. Relay Media Player Synchronization, User Presence, and Independent Camera/Mic States
     * Inbound: /app/room/{roomCode}/sync
     * Outbound: /topic/room/{roomCode}/stream
     */
    @MessageMapping("/room/{roomCode}/sync")
    public void handleVideoSync(@DestinationVariable String roomCode, @Payload String payload) {
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/stream", payload);
    }

    // 1. Relay WebRTC Offer to the room
    @MessageMapping("/room/{roomCode}/webrtc/offer")
    public void handleWebRtcOffer(@DestinationVariable String roomCode, @Payload String payload) {
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/webrtc/offer", payload);
    }

    // 2. Relay WebRTC Answer to the room
    @MessageMapping("/room/{roomCode}/webrtc/answer")
    public void handleWebRtcAnswer(@DestinationVariable String roomCode, @Payload String payload) {
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/webrtc/answer", payload);
    }

    // 3. Relay ICE Candidates between peers
    @MessageMapping("/room/{roomCode}/webrtc/candidate")
    public void handleWebRtcCandidate(@DestinationVariable String roomCode, @Payload String payload) {
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/webrtc/candidate", payload);
    }
}