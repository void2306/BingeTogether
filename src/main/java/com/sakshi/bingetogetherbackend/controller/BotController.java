package com.sakshi.bingetogetherbackend.controller;

import com.sakshi.bingetogetherbackend.dto.BotRequest;
import com.sakshi.bingetogetherbackend.dto.BotResponse;
import com.sakshi.bingetogetherbackend.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bot")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BotController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<BotResponse> askBingeBot(@RequestBody BotRequest request) {
        BotResponse response = aiChatService.askBingeBot(request);
        return ResponseEntity.ok(response);
    }
}