package com.example.pulsechat.service;

import com.example.pulsechat.dto.ChatMessageEvent;
import com.example.pulsechat.dto.MessageResponse;
import com.example.pulsechat.dto.TypingEvent;
import com.example.pulsechat.entity.ChatRoom;
import com.example.pulsechat.entity.Message;
import com.example.pulsechat.exception.RoomNotFoundException;
import com.example.pulsechat.repository.ChatRoomRepository;
import com.example.pulsechat.repository.MessageRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Core messaging logic. Chat messages are persisted to Postgres and then
 * fanned out through Redis pub/sub; typing indicators are ephemeral and
 * only broadcast locally via the in-memory broker.
 */
@Service
public class MessageService {

    private static final int MAX_CONTENT_LENGTH = 2000;

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ChatEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(ChatRoomRepository chatRoomRepository,
                          MessageRepository messageRepository,
                          ChatEventPublisher eventPublisher,
                          SimpMessagingTemplate messagingTemplate) {
        this.chatRoomRepository = chatRoomRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Persists the message and publishes it for broadcast. The Redis listener
     * ({@link com.example.pulsechat.config.ChatRedisListener}) picks it up and
     * pushes it to {@code /topic/rooms/{roomId}} on every app instance.
     */
    @Transactional
    public MessageResponse sendMessage(Long roomId, String sender, String content) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
        if (sender == null || sender.isBlank()) {
            throw new IllegalArgumentException("sender must be present");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("content must be at most 2000 characters");
        }

        Message saved = messageRepository.save(Message.builder()
                .room(room)
                .sender(sender)
                .content(content)
                .build());

        eventPublisher.publish(ChatMessageEvent.of(saved));
        return MessageResponse.from(saved);
    }

    /**
     * Ephemeral typing indicator. Deliberately NOT persisted and NOT fanned
     * out through Redis — it only reaches clients connected to this instance.
     */
    public void broadcastTyping(Long roomId, String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must be present");
        }
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/typing",
                new TypingEvent(roomId, username, Instant.now()));
    }
}
