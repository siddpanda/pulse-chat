package com.example.pulsechat.dto;

import com.example.pulsechat.entity.Message;

import java.time.Instant;

/**
 * Internal event published on the Redis {@code "chat"} channel so every
 * app instance can broadcast the message to its own WebSocket clients.
 */
public record ChatMessageEvent(Long roomId, Long messageId, String sender, String content, Instant sentAt) {

    public static ChatMessageEvent of(Message message) {
        return new ChatMessageEvent(
                message.getRoom().getId(),
                message.getId(),
                message.getSender(),
                message.getContent(),
                message.getSentAt());
    }
}
