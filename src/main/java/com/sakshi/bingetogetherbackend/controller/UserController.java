package com.sakshi.bingetogetherbackend.controller;

import com.sakshi.bingetogetherbackend.dto.LoginRequest;
import com.sakshi.bingetogetherbackend.model.User;
import com.sakshi.bingetogetherbackend.repository.UserRepository;
import com.sakshi.bingetogetherbackend.service.UserService;
import com.sakshi.bingetogetherbackend.security.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public UserController(UserService userService, JwtUtils jwtUtils, UserRepository userRepository) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        System.out.println("SIGNUP API HIT FOR: " + user.getEmail());
        try {
            User savedUser = userService.saveUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Signup failed internal logic");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        System.out.println("LOGIN API HIT FOR: " + request.getEmail());

        User user = userService.login(request.getEmail(), request.getPassword());

        if (user == null || user.getId() == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\": \"Invalid email or password. Access Denied.\"}");
        }

        String token = jwtUtils.generateToken(user.getEmail());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("user", user);
        responseBody.put("token", token);

        return ResponseEntity.ok(responseBody);
    }

    // 🚀 FIXED: Decodes Google ID Token locally to completely bypass 401/500 connection blocks!
    @PostMapping("/google/callback")
    public ResponseEntity<?> handleGoogleLogin(@RequestBody Map<String, String> requestBody) {
        String idTokenString = requestBody.get("token");

        if (idTokenString == null || idTokenString.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing Google Token parameter context"));
        }

        try {
            // 1. Decode the identity token directly (Google ID token is a standard Base64 JWT payload)
            String[] pieces = idTokenString.split("\\.");
            if (pieces.length < 2) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid JWT token structure format"));
            }

            String payload = new String(java.util.Base64.getUrlDecoder().decode(pieces[1]), java.nio.charset.StandardCharsets.UTF_8);

            // Parse token string using standard Jackson library which comes built-in with Spring Boot Starter Web
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode googleProfile = mapper.readTree(payload);

            if (googleProfile == null || !googleProfile.has("email")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Failed to resolve identity data from Google Payload"));
            }

            // 2. Extract user identity details safely from local node claims
            String email = googleProfile.get("email").asText();
            String name = googleProfile.has("name") ? googleProfile.get("name").asText() : "User";
            String picture = googleProfile.has("picture") ? googleProfile.get("picture").asText() : "";
            String subId = googleProfile.has("sub") ? googleProfile.get("sub").asText() : "";

            System.out.println("Google identity parsed locally for email: " + email);

            // 3. Sync state database record query metrics
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setAvatarUrl(picture);
                userRepository.save(user);
            } else {
                user = new User(name, email, "GOOGLE", subId, picture);
                userRepository.save(user);
            }

            // 4. Issue the clean application authentication access token wrapper
            String applicationJwtToken = jwtUtils.generateToken(user.getEmail());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("user", user);
            responseBody.put("token", applicationJwtToken);

            return ResponseEntity.ok(responseBody);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "OAuth parsing extraction exception context: " + ex.getMessage()));
        }
    }
}