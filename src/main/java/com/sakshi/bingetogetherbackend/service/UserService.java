package com.sakshi.bingetogetherbackend.service;

import com.sakshi.bingetogetherbackend.model.User;
import com.sakshi.bingetogetherbackend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getWelcomeMessage() {
        return "Welcome to BingeTogether";
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User updateUser(Long id, User updatedUser) {

        User existingUser = userRepository.findById(id).orElse(null);

        if (existingUser == null) {
            return null;
        }

        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPassword(updatedUser.getPassword());

        return userRepository.save(existingUser);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // 🔥 FIXED LOGIN (important part)
    public User login(String email, String password) {

        Optional<User> user =
                userRepository.findByEmailAndPassword(email, password);

        // IMPORTANT: avoid returning null (frontend crashes on JSON parse)
        if (user.isEmpty()) {
            return new User(); // safe empty object instead of null
        }

        return user.get();
    }
}