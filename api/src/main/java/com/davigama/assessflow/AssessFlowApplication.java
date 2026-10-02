package com.davigama.assessflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
        exclude = UserDetailsServiceAutoConfiguration.class,
        excludeName = {
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
                "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration",
                "org.springframework.boot.health.autoconfigure.redis.RedisHealthContributorAutoConfiguration"
        })
public class AssessFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssessFlowApplication.class, args);
    }
}
