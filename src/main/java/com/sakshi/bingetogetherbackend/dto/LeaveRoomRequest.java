package com.sakshi.bingetogetherbackend.dto;

public class LeaveRoomRequest {

    private Long roomId;
    private Long userId;

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