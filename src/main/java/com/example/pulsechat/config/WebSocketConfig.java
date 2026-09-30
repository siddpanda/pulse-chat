package com.example.pulsechat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket setup:
 * <ul>
 *   <li>Handshake endpoint: {@code /ws} (SockJS fallback enabled for old browsers/proxies)</li>
 *   <li>Client-to-server: {@code /app/...} → routed to {@code @MessageMapping} methods</li>
 *   <li>Server-to-client: {@code /topic/...} → simple in-memory broker</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthHandshakeInterceptor authHandshakeInterceptor;

    public WebSocketConfig(AuthHandshakeInterceptor authHandshakeInterceptor) {
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOriginPatterns("*") // demo only; lock this down in production
                .withSockJS();
    }
}
