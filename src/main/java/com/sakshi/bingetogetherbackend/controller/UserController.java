package com.sakshi.bingetogetherbackend.controller;

import com.sakshi.bingetogetherbackend.model.User;
import com.sakshi.bingetogetherbackend.service.UserService;
import org.springframework.web.bind.annotation.*;
import com.sakshi.bingetogetherbackend.dto.LoginRequest;
@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public User signup(@RequestBody User user) {
        return userService.saveUser(user);
    }
//    @PostMapping("/login")
//    public User login(
//            @RequestBody LoginRequest request) {
//
//        return userService.login(
//                request.getEmail(),
//                request.getPassword()
//        );
//    }

    @PostMapping("/login")
    public User login(@RequestBody LoginRequest request) {

        System.out.println(request.getEmail());
        System.out.println(request.getPassword());
        System.out.println("LOGIN API HIT");

        return userService.login(
                request.getEmail(),
                request.getPassword()
        );
    }

}
