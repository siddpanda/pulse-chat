package com.example.pulsechat.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for {@code /app/chat.typing}. Broadcast only — never persisted.
 */
public record TypingRequest(
        @NotNull(message = "roomId must be present")
        Long roomId
) {}
