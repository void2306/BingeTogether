package com.sakshi.bingetogetherbackend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "room_member")
public class RoomMember {

    // 1. Embedded Composite ID wrapper containing (roomId, userId)
    @EmbeddedId
    private RoomMemberId id;

    @Column(name = "role")
    private String role;

    @Column(name = "nickname")
    private String nickname;

    // 2. Maps the connection back to the Room table for Hibernate joins
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomId") // Links directly to the roomId inside RoomMemberId
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // Default Constructor required by JPA
    public RoomMember() {}

    public RoomMember(RoomMemberId id, String role, String nickname, Room room) {
        this.id = id;
        this.role = role;
        this.nickname = nickname;
        this.room = room;
    }

    // ==========================================
    // GETTERS AND SETTERS (The missing methods!)
    // ==========================================

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

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}