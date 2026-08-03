package com.sakshi.bingetogetherbackend.service;

import com.sakshi.bingetogetherbackend.dto.BotRequest;
import com.sakshi.bingetogetherbackend.dto.BotResponse;
import com.sakshi.bingetogetherbackend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final RoomRepository roomRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public BotResponse askBingeBot(BotRequest request) {
        // 1. Context fallback for video title
        String videoTitle = "Watch Party Stream";

        // 2. Build Prompt (System Rules + Context + User Message)
        String promptText = String.format(
                "You are BingeBot, an enthusiastic watch-party assistant in a live chat room.\n" +
                        "Current Video context: %s\n" +
                        "User timestamp in video: %.1f seconds\n" +
                        "User asked: %s\n" +
                        "Keep your response concise (maximum 3 sentences).",
                videoTitle,
                request.getCurrentTimestamp() != null ? request.getCurrentTimestamp() : 0.0,
                request.getUserMessage()
        );

        // 3. Construct Gemini JSON Payload
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", promptText)))
                )
        );

        try {
            // 4. Send HTTP Request to Gemini API
            RestClient restClient = RestClient.create();
            Map<?, ?> response = restClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            // 5. Extract answer text
            String botAnswer = extractAnswerFromGeminiResponse(response);

            return BotResponse.builder()
                    .roomId(request.getRoomId())
                    .sender("BingeBot")
                    .answer(botAnswer)
                    .timestamp(LocalDateTime.now())
                    .isError(false)
                    .build();

        } catch (Exception e) {
            log.error("Error communicating with Gemini API: ", e);
            return BotResponse.builder()
                    .roomId(request.getRoomId())
                    .sender("BingeBot")
                    .answer("Oops! BingeBot ran into a temporary glitch. Try asking again!")
                    .timestamp(LocalDateTime.now())
                    .isError(true)
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractAnswerFromGeminiResponse(Map<?, ?> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            log.error("Failed to parse Gemini API JSON response: ", e);
            return "Unable to process AI response payload.";
        }
    }
}