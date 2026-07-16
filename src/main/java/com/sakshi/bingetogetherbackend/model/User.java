package com.sakshi.bingetogetherbackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;
    private String password;
    private String provider; // "GOOGLE" ya "LOCAL"
    private String providerId; // Google ki unique user numeric ID
    private String avatarUrl; // User ki Google profile picture link

    // 1. Default constructor required by Hibernate
    public User() {
    }

    // 2. Clear Constructor WITHOUT ID (Crucial for handling JSON registration requests!)
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // 3. Full Parameterized constructor
    public User(Long id, String username, String email, String password) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // 4. Extended Constructor explicitly optimized for OAuth registration loops
    public User(String username, String email, String provider, String providerId, String avatarUrl) {
        this.username = username;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.avatarUrl = avatarUrl;
    }

    // --- STANDARD GETTERS AND SETTERS ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // --- OAUTH EXTENDED GETTERS AND SETTERS ---
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}