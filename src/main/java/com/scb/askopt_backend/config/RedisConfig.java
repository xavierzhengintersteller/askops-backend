package com.scb.askopt_backend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use the new JacksonJsonRedisSerializer
        JacksonJsonRedisSerializer<Object> serializer = new JacksonJsonRedisSerializer<>(Object.class);

//        // Customize the ObjectMapper if needed (using Jackson 3's tools.jackson.databind.ObjectMapper)
//        ObjectMapper customMapper = new ObjectMapper();
//        // ... further configuration
//        serializer.setObjectMapper(customMapper); // Use constructor for configuration in modern Spring Data Redis

        template.setDefaultSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }
}