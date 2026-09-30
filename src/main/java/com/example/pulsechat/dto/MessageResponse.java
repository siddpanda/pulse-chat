package com.example.pulsechat.dto;

import com.example.pulsechat.entity.Message;

import java.time.Instant;

/**
 * Message as seen by API and WebSocket clients.
 */
public record MessageResponse(Long id, Long roomId, String sender, String content, Instant sentAt) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getSender(),
                message.getContent(),
                message.getSentAt());
    }

    public static MessageResponse from(ChatMessageEvent event) {
        return new MessageResponse(
                event.messageId(),
                event.roomId(),
                event.sender(),
                event.content(),
                event.sentAt());
    }
}
