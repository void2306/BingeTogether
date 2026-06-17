package com.sakshi.bingetogetherbackend.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity
public class RoomMember {

    @EmbeddedId
    private RoomMemberId id;

    private String role; // HOST or MEMBER

    private String nickname;

    public RoomMember() {
    }

    public RoomMember(RoomMemberId id, String role, String nickname) {
        this.id = id;
        this.role = role;
        this.nickname = nickname;
    }

    public RoomMemberId getId() {
        return id;
    }

    public void setId(RoomMemberId id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}