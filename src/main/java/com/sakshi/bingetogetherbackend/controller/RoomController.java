package com.sakshi.bingetogetherbackend.controller;

import com.sakshi.bingetogetherbackend.dto.*;
import com.sakshi.bingetogetherbackend.model.ChatMessage;
import com.sakshi.bingetogetherbackend.model.RoomMember;
import com.sakshi.bingetogetherbackend.model.Room;
import com.sakshi.bingetogetherbackend.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // ==========================================
    // ROOM MANAGEMENT ENDPOINTS
    // ==========================================

    /**
     * 🔑 FIXED: Handles both singular and plural mapping from Frontend routing path!
     * Matches: POST http://localhost:8080/room/create OR http://localhost:8080/rooms/create
     */
    @PostMapping(value = {"/room/create", "/rooms/create"})
    public ResponseEntity<Room> createRoom(@RequestBody CreateRoomRequest request) {
        System.out.println("[API] Creating room space for user ID: " + request.getUserId());
        Room savedRoom = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRoom);
    }

    /**
     * Matches: POST http://localhost:8080/room/join
     */
    @PostMapping("/room/join")
    public ResponseEntity<?> joinRoom(@RequestBody JoinRoomRequest request) {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            System.out.println("[API] User ID " + request.getUserId() + " joining room: " + request.getRoomCode());
            String serviceResult = roomService.joinRoom(request.getRoomCode(), request.getUserId());

            responseBody.put("success", true);
            responseBody.put("message", serviceResult);
            responseBody.put("roomCode", request.getRoomCode());

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            e.printStackTrace();
            responseBody.put("success", false);
            responseBody.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }
    }

    /**
     * Matches: GET http://localhost:8080/room/{roomCode}
     */
    @GetMapping("/room/{roomCode}")
    public ResponseEntity<Room> getRoom(@PathVariable String roomCode) {
        return ResponseEntity.ok(roomService.getRoomByCode(roomCode));
    }

    /**
     * Matches: GET http://localhost:8080/room/{roomCode}/members
     */
    @GetMapping("/room/{roomCode}/members")
    public ResponseEntity<List<RoomMember>> getRoomMembers(@PathVariable String roomCode) {
        return ResponseEntity.ok(roomService.getRoomMembers(roomCode));
    }

    /**
     * Matches: PUT http://localhost:8080/room/nickname
     */
    @PutMapping("/room/nickname")
    public ResponseEntity<String> updateNickname(@RequestBody UpdateNicknameRequest request) {
        String result = roomService.updateNickname(request);
        return ResponseEntity.ok("{\"message\": \"" + result + "\"}");
    }

    /**
     * Matches: DELETE http://localhost:8080/room/leave
     */
    @DeleteMapping("/room/leave")
    public ResponseEntity<String> leaveRoom(@RequestBody LeaveRoomRequest request) {
        String result = roomService.leaveRoom(request);
        return ResponseEntity.ok("{\"message\": \"" + result + "\"}");
    }

    // ==========================================
    // CHAT BACKPLANE ENDPOINTS
    // ==========================================

    /**
     * Matches: POST http://localhost:8080/chat/send
     */
    @PostMapping("/chat/send")
    public ResponseEntity<String> sendMessage(@RequestBody SendMessageRequest request) {
        String result = roomService.sendMessage(request);
        return ResponseEntity.ok("{\"message\": \"" + result + "\"}");
    }

    /**
     * Matches: GET http://localhost:8080/chat/{roomId}
     */
    @GetMapping("/chat/{roomId}")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getMessages(roomId));
    }

    // ==========================================
    // USER PERSISTENCE ENDPOINTS
    // ==========================================

    /**
     * Matches: GET http://localhost:8080/user/{userId}/rooms
     */
    @GetMapping("/user/{userId}/rooms")
    public ResponseEntity<List<Room>> getUserRooms(@PathVariable Long userId) {
        return ResponseEntity.ok(roomService.getUserRooms(userId));
    }
}