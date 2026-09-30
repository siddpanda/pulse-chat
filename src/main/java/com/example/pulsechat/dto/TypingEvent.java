package com.example.pulsechat.dto;

import java.time.Instant;

/**
 * Ephemeral typing indicator broadcast to
 * {@code /topic/rooms/{roomId}/typing}. Not persisted anywhere.
 */
public record TypingEvent(Long roomId, String username, Instant at) {}
