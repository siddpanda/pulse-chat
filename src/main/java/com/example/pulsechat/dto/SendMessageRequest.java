package com.example.pulsechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code /app/chat.send}. The sender is taken from the
 * WebSocket session (handshake), never trusted from the client body.
 */
public record SendMessageRequest(
        @NotNull(message = "roomId must be present")
        Long roomId,

        @NotBlank(message = "content must not be blank")
        @Size(max = 2000, message = "content must be at most 2000 characters")
        String content
) {}
