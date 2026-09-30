package com.example.pulsechat.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Presence tracking backed by Redis. Each room has a set
 * {@code presence:{roomId}} holding the usernames currently online.
 *
 * <p>Clients send a heartbeat every ~30s; each heartbeat refreshes the
 * key's TTL (60s). If a client disconnects without saying goodbye, its
 * entry simply expires — no explicit "leave" needed.
 */
@Service
public class PresenceService {

    private static final Duration PRESENCE_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    public PresenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void heartbeat(Long roomId, String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must be present");
        }
        String key = key(roomId);
        redisTemplate.opsForSet().add(key, username);
        redisTemplate.expire(key, PRESENCE_TTL);
    }

    public List<String> onlineUsers(Long roomId) {
        Set<String> members = redisTemplate.opsForSet().members(key(roomId));
        if (members == null) {
            return List.of();
        }
        return members.stream().sorted().toList();
    }

    public void clear(Long roomId) {
        redisTemplate.delete(key(roomId));
    }

    private String key(Long roomId) {
        return "presence:" + roomId;
    }
}
