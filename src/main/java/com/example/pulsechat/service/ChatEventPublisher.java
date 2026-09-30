package com.example.pulsechat.service;

import com.example.pulsechat.dto.ChatMessageEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes chat events to the Redis {@code "chat"} channel.
 * Every application instance subscribes (see {@link com.example.pulsechat.config.ChatRedisListener})
 * and forwards received events to its own WebSocket clients — this is what
 * makes broadcast work when the app is scaled horizontally.
 */
@Component
public class ChatEventPublisher {

    public static final String CHANNEL = "chat";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ChatEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(ChatMessageEvent event) {
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize chat event", e);
        }
    }
}
