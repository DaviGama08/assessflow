package com.davigama.assessflow.shared.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisConfig {
    @Bean
    LettuceConnectionFactory redisConnectionFactory(
            @Value("${app.redis.host:localhost}") String host,
            @Value("${app.redis.port:6379}") int port,
            @Value("${app.redis.password:}") String password) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            standalone.setPassword(password);
        }
        LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(250))
                .build();
        return new LettuceConnectionFactory(standalone, client);
    }

    @Bean
    StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
