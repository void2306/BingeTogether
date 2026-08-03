package com.sakshi.bingetogetherbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BotRequest {

    @NotBlank(message = "Room ID is required")
    private String roomId;

    @NotBlank(message = "User message cannot be empty")
    private String userMessage;

    private Double currentTimestamp;
}