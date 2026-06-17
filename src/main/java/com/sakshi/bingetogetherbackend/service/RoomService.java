package com.sakshi.bingetogetherbackend.service;
import com.sakshi.bingetogetherbackend.dto.CreateRoomRequest;
import com.sakshi.bingetogetherbackend.dto.LeaveRoomRequest;
import com.sakshi.bingetogetherbackend.dto.SendMessageRequest;
import com.sakshi.bingetogetherbackend.dto.UpdateNicknameRequest;
import com.sakshi.bingetogetherbackend.model.ChatMessage;
import com.sakshi.bingetogetherbackend.model.Room;
import com.sakshi.bingetogetherbackend.model.RoomMember;
import com.sakshi.bingetogetherbackend.model.RoomMemberId;
import com.sakshi.bingetogetherbackend.repository.ChatMessageRepository;
import com.sakshi.bingetogetherbackend.repository.RoomMemberRepository;
import com.sakshi.bingetogetherbackend.repository.RoomRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;

    public RoomService(RoomRepository roomRepository,
                       RoomMemberRepository roomMemberRepository,
                       ChatMessageRepository chatMessageRepository) {

        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public Room createRoom(CreateRoomRequest request) {

        Room room = new Room();

        room.setRoomName(request.getRoomName());
        room.setRoomType(request.getRoomType());
        room.setMovieLink(request.getMovieLink());

        String roomCode = room.getRoomName()
                .toLowerCase()
                .replace(" ", "_")
                + ((int) (Math.random() * 9000) + 1000);

        room.setRoomCode(roomCode);
        room.setActive(true);

        Room savedRoom = roomRepository.save(room);

        RoomMemberId memberId =
                new RoomMemberId(
                        savedRoom.getId(),
                        request.getUserId()
                );

        RoomMember host = new RoomMember();
        host.setId(memberId);
        host.setRole("HOST");

        roomMemberRepository.save(host);

        return savedRoom;
    }

    public String joinRoom(String roomCode, Long userId) {

        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        RoomMemberId memberId =
                new RoomMemberId(
                        room.getId(),
                        userId
                );

        if (roomMemberRepository.existsById(memberId)) {
            return "User already joined this room";
        }

        RoomMember member = new RoomMember();
        member.setId(memberId);
        member.setRole("MEMBER");

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

    public String updateNickname(UpdateNicknameRequest request) {

        RoomMemberId memberId =
                new RoomMemberId(
                        request.getRoomId(),
                        request.getUserId()
                );

        RoomMember member = roomMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        member.setNickname(request.getNickname());

        roomMemberRepository.save(member);

        return "Nickname updated successfully";
    }

    public String leaveRoom(LeaveRoomRequest request) {

        RoomMemberId memberId =
                new RoomMemberId(
                        request.getRoomId(),
                        request.getUserId()
                );

        if (!roomMemberRepository.existsById(memberId)) {
            return "Member not found in room";
        }

        roomMemberRepository.deleteById(memberId);

        return "Left room successfully";
    }

    public String sendMessage(SendMessageRequest request) {

        ChatMessage chatMessage = new ChatMessage();

        chatMessage.setRoomId(request.getRoomId());
        chatMessage.setUserId(request.getUserId());
        chatMessage.setMessage(request.getMessage());

        chatMessageRepository.save(chatMessage);

        return "Message sent successfully";
    }

    public List<ChatMessage> getMessages(Long roomId) {

        return chatMessageRepository.findByRoomId(roomId);
    }
    public List<Room> getUserRooms(Long userId) {

        List<RoomMember> memberships =
                roomMemberRepository.findByIdUserId(userId);

        List<Long> roomIds = memberships.stream()
                .map(member -> member.getId().getRoomId())
                .toList();

        return roomRepository.findAllById(roomIds);
    }
}