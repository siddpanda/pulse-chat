package com.example.pulsechat.controller;

import com.example.pulsechat.dto.ChatRoomResponse;
import com.example.pulsechat.dto.CreateRoomRequest;
import com.example.pulsechat.dto.MessageResponse;
import com.example.pulsechat.entity.Message;
import com.example.pulsechat.repository.MessageRepository;
import com.example.pulsechat.service.ChatRoomService;
import com.example.pulsechat.service.PresenceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final MessageRepository messageRepository;
    private final PresenceService presenceService;

    public ChatRoomController(ChatRoomService chatRoomService,
                              MessageRepository messageRepository,
                              PresenceService presenceService) {
        this.chatRoomService = chatRoomService;
        this.messageRepository = messageRepository;
        this.presenceService = presenceService;
    }

    @PostMapping
    public ResponseEntity<ChatRoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        ChatRoomResponse room = chatRoomService.createRoom(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @GetMapping
    public List<ChatRoomResponse> listRooms() {
        return chatRoomService.listRooms();
    }

    /**
     * Message history, newest first. Example:
     * {@code GET /api/rooms/1/messages?page=0&size=20}
     */
    @GetMapping("/{id}/messages")
    public Page<MessageResponse> messageHistory(@PathVariable Long id,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        chatRoomService.getRoom(id); // 404 if the room doesn't exist
        Page<Message> messages = messageRepository.findByRoomIdOrderBySentAtDesc(id, PageRequest.of(page, size));
        return messages.map(MessageResponse::from);
    }

    @GetMapping("/{id}/presence")
    public List<String> presence(@PathVariable Long id) {
        chatRoomService.getRoom(id); // 404 if the room doesn't exist
        return presenceService.onlineUsers(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        chatRoomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
