package com.example.pulsechat.repository;

import com.example.pulsechat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Message history for a room, newest first (chat-style pagination).
     */
    Page<Message> findByRoomIdOrderBySentAtDesc(Long roomId, Pageable pageable);

    void deleteByRoomId(Long roomId);
}
