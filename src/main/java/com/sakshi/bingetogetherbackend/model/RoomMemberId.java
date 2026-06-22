package com.sakshi.bingetogetherbackend.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class RoomMemberId implements Serializable {

    private Long roomId;
    private Long userId;

    public RoomMemberId() {
    }

    public RoomMemberId(Long roomId, Long userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}