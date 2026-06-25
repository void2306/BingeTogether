package com.sakshi.bingetogetherbackend.controller;

import com.sakshi.bingetogetherbackend.dto.LoginRequest;
import com.sakshi.bingetogetherbackend.model.User;
import com.sakshi.bingetogetherbackend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175" , "http://localhost:5176"}, allowCredentials = "true") // Strips down browser network options restrictions
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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

        // FIX: Detects actual failure states cleanly now
        if (user == null || user.getId() == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\": \"Invalid email or password. Access Denied.\"}");
        }

        return ResponseEntity.ok(user);
    }
}