package com.sakshi.bingetogetherbackend.repository;

import com.sakshi.bingetogetherbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}