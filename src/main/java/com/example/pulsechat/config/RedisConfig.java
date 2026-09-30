package com.example.pulsechat.config;

import com.example.pulsechat.service.ChatEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Wires the Redis pub/sub listener. The connection factory itself
 * (Lettuce) is auto-configured by Spring Boot from
 * {@code spring.data.redis.*} properties.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                       ChatRedisListener chatRedisListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(chatRedisListener, new ChannelTopic(ChatEventPublisher.CHANNEL));
        return container;
    }
}
