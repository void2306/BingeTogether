package com.sakshi.bingetogetherbackend.repository;

import com.sakshi.bingetogetherbackend.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoomId(Long roomId);
}