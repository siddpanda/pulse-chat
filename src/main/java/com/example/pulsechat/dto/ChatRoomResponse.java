package com.example.pulsechat.dto;

import com.example.pulsechat.entity.ChatRoom;

import java.time.Instant;

public record ChatRoomResponse(Long id, String name, Instant createdAt) {

    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(room.getId(), room.getName(), room.getCreatedAt());
    }
}
