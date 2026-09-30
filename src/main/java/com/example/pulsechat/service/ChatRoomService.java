package com.example.pulsechat.service;

import com.example.pulsechat.dto.ChatRoomResponse;
import com.example.pulsechat.entity.ChatRoom;
import com.example.pulsechat.exception.RoomNotFoundException;
import com.example.pulsechat.repository.ChatRoomRepository;
import com.example.pulsechat.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Room lifecycle management. Message history and presence state live in
 * their own stores, so deleting a room explicitly cleans both up.
 */
@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final PresenceService presenceService;

    public ChatRoomService(ChatRoomRepository chatRoomRepository,
                           MessageRepository messageRepository,
                           PresenceService presenceService) {
        this.chatRoomRepository = chatRoomRepository;
        this.messageRepository = messageRepository;
        this.presenceService = presenceService;
    }

    @Transactional
    public ChatRoomResponse createRoom(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("room name must not be blank");
        }
        String trimmed = name.trim();
        if (chatRoomRepository.existsByName(trimmed)) {
            throw new IllegalArgumentException("room name already exists: " + trimmed);
        }
        ChatRoom saved = chatRoomRepository.save(ChatRoom.builder().name(trimmed).build());
        return ChatRoomResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> listRooms() {
        return chatRoomRepository.findAll().stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoom getRoom(Long id) {
        return chatRoomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException(id));
    }

    @Transactional
    public void deleteRoom(Long id) {
        ChatRoom room = getRoom(id);
        messageRepository.deleteByRoomId(id);
        presenceService.clear(id);
        chatRoomRepository.delete(room);
    }
}
