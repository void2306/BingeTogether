package com.sakshi.bingetogetherbackend.dto;

public class SyncMessage {
    private String sender;
    private String action; // e.g., "SEEK_REQUEST", "PLAY", "PAUSE"
    private double targetTime; // The exact second index of the video slider

    // Default Constructor
    public SyncMessage() {}

    public SyncMessage(String sender, String action, double targetTime) {
        this.sender = sender;
        this.action = action;
        this.targetTime = targetTime;
    }

    // Getters and Setters
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public double getTargetTime() { return targetTime; }
    public void setTargetTime(double targetTime) { this.targetTime = targetTime; }
}