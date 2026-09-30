package com.example.pulsechat.config;

import com.example.pulsechat.dto.ChatMessageEvent;
import com.example.pulsechat.dto.MessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Subscribes to the Redis {@code "chat"} channel. Every instance of this
 * service receives every published message and forwards it to the STOMP
 * clients connected to <em>this</em> instance via the in-memory broker.
 *
 * <p>With a single instance this is effectively a pass-through; with N
 * instances behind a load balancer it is the fan-out that keeps chat
 * working no matter which instance a client landed on.
 */
@Component
public class ChatRedisListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(ChatRedisListener.class);

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatRedisListener(ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatMessageEvent event = objectMapper.readValue(message.getBody(), ChatMessageEvent.class);
            messagingTemplate.convertAndSend(
                    "/topic/rooms/" + event.roomId(),
                    MessageResponse.from(event));
        } catch (IOException e) {
            log.error("Failed to deserialize chat event from Redis", e);
        }
    }
}
