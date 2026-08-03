package com.sakshi.bingetogetherbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BotResponse {

    private String roomId;
    private String sender;
    private String answer;
    private LocalDateTime timestamp;
    private boolean isError;
}