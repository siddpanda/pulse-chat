package com.example.pulsechat.service;

import com.example.pulsechat.dto.ChatMessageEvent;
import com.example.pulsechat.dto.MessageResponse;
import com.example.pulsechat.dto.TypingEvent;
import com.example.pulsechat.entity.ChatRoom;
import com.example.pulsechat.entity.Message;
import com.example.pulsechat.exception.RoomNotFoundException;
import com.example.pulsechat.repository.ChatRoomRepository;
import com.example.pulsechat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests — no Spring context, no database, no Redis.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ChatEventPublisher eventPublisher;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private MessageService messageService;

    private ChatRoom room;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(chatRoomRepository, messageRepository, eventPublisher, messagingTemplate);
        room = ChatRoom.builder().id(1L).name("general").createdAt(Instant.now()).build();
    }

    @Test
    void sendMessage_persistsThenPublishes() {
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(room));
        Message saved = Message.builder()
                .id(10L).room(room).sender("alice").content("hello").sentAt(Instant.now())
                .build();
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        MessageResponse response = messageService.sendMessage(1L, "alice", "hello");

        // 1. persisted with the right fields
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertEquals("alice", messageCaptor.getValue().getSender());
        assertEquals("hello", messageCaptor.getValue().getContent());
        assertEquals(1L, messageCaptor.getValue().getRoom().getId());

        // 2. published for broadcast (the Redis listener fans it out)
        ArgumentCaptor<ChatMessageEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        ChatMessageEvent event = eventCaptor.getValue();
        assertEquals(1L, event.roomId());
        assertEquals(10L, event.messageId());
        assertEquals("alice", event.sender());
        assertEquals("hello", event.content());

        // 3. response mirrors the saved row
        assertEquals(10L, response.id());
        assertEquals(1L, response.roomId());
        assertEquals("alice", response.sender());
    }

    @Test
    void sendMessage_unknownRoom_throwsAndDoesNothing() {
        when(chatRoomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class,
                () -> messageService.sendMessage(99L, "alice", "hello"));

        verifyNoInteractions(messageRepository, eventPublisher);
    }

    @Test
    void sendMessage_blankContent_throwsAndDoesNotPersist() {
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThrows(IllegalArgumentException.class,
                () -> messageService.sendMessage(1L, "alice", "   "));

        verify(messageRepository, never()).save(any(Message.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void sendMessage_overlongContent_throwsAndDoesNotPersist() {
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThrows(IllegalArgumentException.class,
                () -> messageService.sendMessage(1L, "alice", "x".repeat(2001)));

        verify(messageRepository, never()).save(any(Message.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void broadcastTyping_sendsToTopicAndDoesNotPersist() {
        messageService.broadcastTyping(1L, "alice");

        ArgumentCaptor<TypingEvent> captor = ArgumentCaptor.forClass(TypingEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/1/typing"), captor.capture());
        assertEquals(1L, captor.getValue().roomId());
        assertEquals("alice", captor.getValue().username());

        // typing is ephemeral: nothing hits the database or Redis pub/sub
        verifyNoInteractions(messageRepository, chatRoomRepository, eventPublisher);
    }
}
