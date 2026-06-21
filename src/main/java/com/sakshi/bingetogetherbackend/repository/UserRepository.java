package com.sakshi.bingetogetherbackend.repository;

import com.sakshi.bingetogetherbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndPassword(
            String email,
            String password
    );
}