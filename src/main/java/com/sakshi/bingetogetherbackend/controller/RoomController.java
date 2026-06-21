package com.sakshi.bingetogetherbackend.controller;
import com.sakshi.bingetogetherbackend.dto.*;
import com.sakshi.bingetogetherbackend.model.ChatMessage;
import com.sakshi.bingetogetherbackend.model.RoomMember;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.sakshi.bingetogetherbackend.model.Room;
import com.sakshi.bingetogetherbackend.service.RoomService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/room/create")
    public Room createRoom(@RequestBody CreateRoomRequest request) {
        return roomService.createRoom(request);
    }

    @PostMapping("/room/join")
    public String joinRoom(@RequestBody JoinRoomRequest request) {
        return roomService.joinRoom(
                request.getRoomCode(),
                request.getUserId()
        );
    }
    @GetMapping("/room/{roomCode}")
    public Room getRoom(@PathVariable String roomCode) {

        return roomService.getRoomByCode(roomCode);
    }
    @GetMapping("/room/{roomCode}/members")
    public List<RoomMember> getRoomMembers(
            @PathVariable String roomCode) {

        return roomService.getRoomMembers(roomCode);
    }
    @PutMapping("/room/nickname")
    public String updateNickname(
            @RequestBody UpdateNicknameRequest request) {

        return roomService.updateNickname(request);
    }
    @DeleteMapping("/room/leave")
    public String leaveRoom(
            @RequestBody LeaveRoomRequest request) {

        return roomService.leaveRoom(request);
    }
    @PostMapping("/chat/send")
    public String sendMessage(
            @RequestBody SendMessageRequest request) {

        return roomService.sendMessage(request);
    }
    @GetMapping("/chat/{roomId}")
    public List<ChatMessage> getMessages(
            @PathVariable Long roomId) {

        return roomService.getMessages(roomId);
    }
    @GetMapping("/user/{userId}/rooms")
    public List<Room> getUserRooms(
            @PathVariable Long userId) {

        return roomService.getUserRooms(userId);
    }
}