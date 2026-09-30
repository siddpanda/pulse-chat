package com.example.pulsechat.service;

import com.example.pulsechat.dto.ChatRoomResponse;
import com.example.pulsechat.entity.ChatRoom;
import com.example.pulsechat.exception.RoomNotFoundException;
import com.example.pulsechat.repository.ChatRoomRepository;
import com.example.pulsechat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests — no Spring context, no database, no Redis.
 */
@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private PresenceService presenceService;

    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() {
        chatRoomService = new ChatRoomService(chatRoomRepository, messageRepository, presenceService);
    }

    @Test
    void createRoom_savesAndReturns() {
        when(chatRoomRepository.existsByName("general")).thenReturn(false);
        ChatRoom saved = ChatRoom.builder().id(1L).name("general").createdAt(Instant.now()).build();
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(saved);

        ChatRoomResponse response = chatRoomService.createRoom("general");

        assertEquals(1L, response.id());
        assertEquals("general", response.name());
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void createRoom_blankName_throws() {
        assertThrows(IllegalArgumentException.class, () -> chatRoomService.createRoom("  "));

        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void createRoom_duplicateName_throws() {
        when(chatRoomRepository.existsByName("general")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> chatRoomService.createRoom("general"));

        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void listRooms_mapsAll() {
        ChatRoom room = ChatRoom.builder().id(1L).name("general").createdAt(Instant.now()).build();
        when(chatRoomRepository.findAll()).thenReturn(List.of(room));

        List<ChatRoomResponse> rooms = chatRoomService.listRooms();

        assertEquals(1, rooms.size());
        assertEquals("general", rooms.get(0).name());
    }

    @Test
    void getRoom_missing_throws() {
        when(chatRoomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class, () -> chatRoomService.getRoom(99L));
    }

    @Test
    void deleteRoom_cleansUpMessagesPresenceAndRoom() {
        ChatRoom room = ChatRoom.builder().id(1L).name("general").createdAt(Instant.now()).build();
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(room));

        chatRoomService.deleteRoom(1L);

        verify(messageRepository).deleteByRoomId(1L);
        verify(presenceService).clear(1L);
        verify(chatRoomRepository).delete(room);
    }

    @Test
    void deleteRoom_missing_throwsAndDeletesNothing() {
        when(chatRoomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class, () -> chatRoomService.deleteRoom(99L));

        verifyNoInteractions(messageRepository, presenceService);
        verify(chatRoomRepository, never()).delete(any(ChatRoom.class));
    }
}
