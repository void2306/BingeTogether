package com.sakshi.bingetogetherbackend.controller;

import com.sakshi.bingetogetherbackend.dto.LoginRequest;
import com.sakshi.bingetogetherbackend.model.User;
import com.sakshi.bingetogetherbackend.service.UserService;
import com.sakshi.bingetogetherbackend.security.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    public UserController(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        System.out.println("SIGNUP API HIT FOR: " + user.getEmail());
        try {
            User savedUser = userService.saveUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (Exception e) {
            e.printStackTrace(); // 🔥 Railway logs mein exact error message print karega
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

        String token = jwtUtils.generateToken(user.getUsername());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("user", user);
        responseBody.put("token", token);

        return ResponseEntity.ok(responseBody);
    }
}