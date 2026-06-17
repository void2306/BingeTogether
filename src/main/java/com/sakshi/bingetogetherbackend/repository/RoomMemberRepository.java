package com.sakshi.bingetogetherbackend.repository;

import com.sakshi.bingetogetherbackend.model.RoomMember;
import com.sakshi.bingetogetherbackend.model.RoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {
    List<RoomMember> findByIdRoomId(Long roomId);
    List<RoomMember> findByIdUserId(Long userId);
}