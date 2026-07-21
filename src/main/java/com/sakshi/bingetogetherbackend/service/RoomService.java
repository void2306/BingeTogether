package com.sakshi.bingetogetherbackend.service;

import com.sakshi.bingetogetherbackend.dto.CreateRoomRequest;
import com.sakshi.bingetogetherbackend.dto.LeaveRoomRequest;
import com.sakshi.bingetogetherbackend.dto.SendMessageRequest;
import com.sakshi.bingetogetherbackend.dto.UpdateNicknameRequest;
import com.sakshi.bingetogetherbackend.model.ChatMessage;
import com.sakshi.bingetogetherbackend.model.Room;
import com.sakshi.bingetogetherbackend.model.RoomMember;
import com.sakshi.bingetogetherbackend.model.RoomMemberId;
import com.sakshi.bingetogetherbackend.model.User;
import com.sakshi.bingetogetherbackend.repository.ChatMessageRepository;
import com.sakshi.bingetogetherbackend.repository.RoomMemberRepository;
import com.sakshi.bingetogetherbackend.repository.RoomRepository;
import com.sakshi.bingetogetherbackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public RoomService(RoomRepository roomRepository,
                       RoomMemberRepository roomMemberRepository,
                       ChatMessageRepository chatMessageRepository,
                       UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Room createRoom(CreateRoomRequest request) {
        System.out.println("================================");
        System.out.println("ROOM NAME = " + request.getRoomName());
        System.out.println("ROOM TYPE = " + request.getRoomType());
        System.out.println("MOVIE LINK = " + request.getMovieLink());
        System.out.println("USER ID = " + request.getUserId());
        System.out.println("================================");

        if (request.getUserId() == null || request.getUserId() == 0) {
            throw new IllegalArgumentException("Cannot create a room with an empty or invalid user ID.");
        }

        Room room = new Room();
        room.setRoomName(request.getRoomName());
        room.setRoomType(request.getRoomType());
        room.setMovieLink(request.getMovieLink());

        String roomCode = request.getRoomName()
                .toLowerCase()
                .replace(" ", "_")
                + ((int) (Math.random() * 9000) + 1000);

        room.setRoomCode(roomCode);
        room.setActive(true);

        Room savedRoom = roomRepository.save(room);
        System.out.println("Saved Room ID = " + savedRoom.getId());

        RoomMemberId memberId = new RoomMemberId(savedRoom.getId(), request.getUserId());

        RoomMember host = new RoomMember();
        host.setId(memberId);
        host.setRole("HOST");
        host.setRoom(savedRoom);

        roomMemberRepository.save(host);

        return savedRoom;
    }

    @Transactional
    public String joinRoom(String roomCode, Long userId) {
        if (userId == null || userId == 0) {
            throw new IllegalArgumentException("Cannot join room because the incoming user ID is empty.");
        }

        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found with code: " + roomCode));

        RoomMemberId memberId = new RoomMemberId(room.getId(), userId);

        if (roomMemberRepository.existsById(memberId)) {
            return "User already joined this room";
        }

        RoomMember member = new RoomMember();
        member.setId(memberId);
        member.setRole("MEMBER");
        member.setRoom(room);

        roomMemberRepository.save(member);

        return "Joined room successfully";
    }

    public Room getRoomByCode(String roomCode) {
        return roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }

    public List<RoomMember> getRoomMembers(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        return roomMemberRepository.findByIdRoomId(room.getId());
    }

    @Transactional
    public String updateNickname(UpdateNicknameRequest request) {
        RoomMemberId memberId = new RoomMemberId(request.getRoomId(), request.getUserId());

        RoomMember member = roomMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        member.setNickname(request.getNickname());
        roomMemberRepository.save(member);

        return "Nickname updated successfully";
    }

    @Transactional
    public String leaveRoom(LeaveRoomRequest request) {
        RoomMemberId memberId = new RoomMemberId(request.getRoomId(), request.getUserId());

        if (!roomMemberRepository.existsById(memberId)) {
            return "Member not found in room";
        }

        roomMemberRepository.deleteById(memberId);
        return "Left room successfully";
    }

    @Transactional
    public String sendMessage(SendMessageRequest request) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setRoomId(request.getRoomId());
        chatMessage.setUserId(request.getUserId());
        chatMessage.setMessage(request.getMessage());

        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            chatMessage.setUsername(request.getUsername().trim());
        } else if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId()).orElse(null);
            chatMessage.setUsername(user != null ? user.getUsername() : "User");
        } else {
            chatMessage.setUsername("User");
        }

        chatMessageRepository.save(chatMessage);
        return "Message sent successfully";
    }

    public List<ChatMessage> getMessages(Long roomId) {
        return chatMessageRepository.findByRoomId(roomId);
    }

    public List<Room> getUserRooms(Long userId) {
        List<RoomMember> memberships = roomMemberRepository.findByIdUserId(userId);

        List<Long> roomIds = memberships.stream()
                .map(member -> member.getId().getRoomId())
                .toList();

        return roomRepository.findAllById(roomIds);
    }
}