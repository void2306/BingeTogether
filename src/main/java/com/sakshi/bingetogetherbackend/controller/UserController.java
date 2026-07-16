package com.sakshi.bingetogetherbackend.controller;

import com.sakshi.bingetogetherbackend.dto.LoginRequest;
import com.sakshi.bingetogetherbackend.model.User;
import com.sakshi.bingetogetherbackend.repository.UserRepository;
import com.sakshi.bingetogetherbackend.service.UserService;
import com.sakshi.bingetogetherbackend.security.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;

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
    private final RestTemplate restTemplate = new RestTemplate();

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

        // Uses standard primary email mapping string sequence matching JwtUtils update
        String token = jwtUtils.generateToken(user.getEmail());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("user", user);
        responseBody.put("token", token);

        return ResponseEntity.ok(responseBody);
    }

    // 🚀 NEW: Dynamic Google Access Token Authorization Callback Endpoint
    @PostMapping("/google/callback")
    public ResponseEntity<?> handleGoogleLogin(@RequestBody Map<String, String> requestBody) {
        String googleAccessToken = requestBody.get("token");

        if (googleAccessToken == null || googleAccessToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing Google Access Token parameter context"));
        }

        try {
            // 1. Send secure check validation directly to Google profile systems
            String googleUserInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(googleAccessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    googleUserInfoUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> googleProfile = response.getBody();
            if (googleProfile == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Failed to retrieve user profile from Google"));
            }

            // 2. Extract user identity info structure details
            String email = (String) googleProfile.get("email");
            String name = (String) googleProfile.get("name");
            String picture = (String) googleProfile.get("picture");
            String subId = (String) googleProfile.get("sub");

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
                    .body(Map.of("error", "OAuth processing network exception context: " + ex.getMessage()));
        }
    }
}