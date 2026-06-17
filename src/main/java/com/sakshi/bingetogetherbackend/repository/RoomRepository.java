package com.sakshi.bingetogetherbackend.repository;

import com.sakshi.bingetogetherbackend.model.Room;
import com.sakshi.bingetogetherbackend.model.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomCode(String roomCode);
}