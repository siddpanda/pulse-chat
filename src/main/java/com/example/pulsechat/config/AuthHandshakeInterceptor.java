package com.example.pulsechat.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Demo-grade handshake auth: reads {@code ?username=...} from the WebSocket
 * handshake URL and stashes it in the session attributes, where the
 * {@code @MessageMapping} handlers pick it up.
 *
 * <p><b>Not production auth.</b> Anyone can claim any username. A real
 * deployment would validate a JWT in the STOMP CONNECT frame
 * ({@code Authorization} header) via a {@code ChannelInterceptor} on the
 * inbound channel and reject unauthenticated CONNECTs there.
 */
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USERNAME_ATTRIBUTE = "username";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String username = httpRequest.getParameter("username");
            if (username == null || username.isBlank()) {
                return false; // reject the handshake: no identity, no socket
            }
            attributes.put(USERNAME_ATTRIBUTE, username.trim());
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // nothing to clean up
    }
}
