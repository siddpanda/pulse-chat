package com.example.pulsechat.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for {@code /app/chat.heartbeat}. Refreshes the sender's
 * presence entry for the room (Redis set with TTL).
 */
public record HeartbeatRequest(
        @NotNull(message = "roomId must be present")
        Long roomId
) {}
