package com.sakshi.bingetogetherbackend.dto;

public class JoinRoomRequest {

    private String roomCode;
    private Long userId;

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
