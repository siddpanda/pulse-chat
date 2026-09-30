package com.example.pulsechat.controller;

import com.example.pulsechat.config.AuthHandshakeInterceptor;
import com.example.pulsechat.dto.ErrorResponse;
import com.example.pulsechat.dto.HeartbeatRequest;
import com.example.pulsechat.dto.SendMessageRequest;
import com.example.pulsechat.dto.TypingRequest;
import com.example.pulsechat.service.MessageService;
import com.example.pulsechat.service.PresenceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * STOMP message handlers (client → {@code /app/...}).
 *
 * <p>The sender's identity comes from the WebSocket session, populated by
 * {@link AuthHandshakeInterceptor} during the handshake — it is never
 * taken from the message body, so a client can't impersonate anyone.
 */
@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final MessageService messageService;
    private final PresenceService presenceService;

    public ChatWebSocketController(MessageService messageService, PresenceService presenceService) {
        this.messageService = messageService;
        this.presenceService = presenceService;
    }

    /**
     * {@code SEND /app/chat.send} → persist + fan out to
     * {@code /topic/rooms/{roomId}} (via Redis pub/sub).
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload @Valid SendMessageRequest request, StompHeaderAccessor headers) {
        messageService.sendMessage(request.roomId(), usernameOf(headers), request.content());
    }

    /**
     * {@code SEND /app/chat.typing} → ephemeral broadcast to
     * {@code /topic/rooms/{roomId}/typing}. Not persisted.
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload @Valid TypingRequest request, StompHeaderAccessor headers) {
        messageService.broadcastTyping(request.roomId(), usernameOf(headers));
    }

    /**
     * {@code SEND /app/chat.heartbeat} → refresh presence for the sender.
     * Clients should send this roughly every 30 seconds.
     */
    @MessageMapping("/chat.heartbeat")
    public void heartbeat(@Payload @Valid HeartbeatRequest request, StompHeaderAccessor headers) {
        presenceService.heartbeat(request.roomId(), usernameOf(headers));
    }

    /**
     * STOMP-side errors (validation failures, unknown room, ...) go back
     * to the sender's private queue instead of blowing up the connection.
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErrorResponse handleWebSocketError(Exception ex, StompHeaderAccessor headers) {
        log.warn("WebSocket handling failed for session {}: {}",
                headers.getSessionId(), ex.getMessage());
        return ErrorResponse.of(400, "Bad Request", ex.getMessage(), "/app");
    }

    private String usernameOf(StompHeaderAccessor headers) {
        Map<String, Object> sessionAttributes = headers.getSessionAttributes();
        Object username = sessionAttributes == null
                ? null
                : sessionAttributes.get(AuthHandshakeInterceptor.USERNAME_ATTRIBUTE);
        if (username == null) {
            throw new IllegalStateException("No username in WebSocket session — handshake auth missing");
        }
        return username.toString();
    }
}
