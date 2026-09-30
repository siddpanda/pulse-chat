package com.example.pulsechat.repository;

import com.example.pulsechat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    boolean existsByName(String name);
}
