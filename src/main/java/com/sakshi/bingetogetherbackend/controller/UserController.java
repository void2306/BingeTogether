package com.sakshi.bingetogetherbackend.controller;

import com.sakshi.bingetogetherbackend.dto.LoginRequest;
import com.sakshi.bingetogetherbackend.model.User;
import com.sakshi.bingetogetherbackend.service.UserService;
import com.sakshi.bingetogetherbackend.security.JwtUtils; // 🔥 Clear path reference to your token engine
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth") // 🚨 Public prefix mapping gate matching SecurityConfig
//@CrossOrigin(origins = "*")
public class UserController {

    // 🌟 Thread-safe, immutable field configurations
    private final UserService userService;
    private final JwtUtils jwtUtils;

    // 🌟 Unified constructor injection pattern for clean application architecture
    public UserController(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/signup")
    public ResponseEntity<User> signup(@RequestBody User user) {
        System.out.println("SIGNUP API HIT FOR: " + user.getEmail());
        User savedUser = userService.saveUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
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

        // 🔥 GENERATE THE SECURITY WRISTBAND: Create a cryptographically signed token string
        String token = jwtUtils.generateToken(user.getUsername());

        // 🔥 BUNDLE RESPONSES: Put the user object and the token together inside a map package
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("user", user);
        responseBody.put("token", token);

        return ResponseEntity.ok(responseBody);
    }
}