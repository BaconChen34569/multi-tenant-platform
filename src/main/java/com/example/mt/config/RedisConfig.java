package com.example.mt.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存配置：把默认的 JDK 序列化（要求实体 implements Serializable）
 * 换成 JSON 序列化，避免实体类必须实现 Serializable，且缓存内容在
 * redis-cli 里可读，便于调试与演示。
 *
 * 注意：本类手动声明的 RedisCacheManager 会"屏蔽" Spring Boot 的
 * SPRING_CACHE_TYPE=none 自动关闭行为——只要这个 bean 存在，@Cacheable
 * 就仍会走 Redis。因此给它加 @ConditionalOnProperty，让 type=none
 * 时本 bean 不创建，Spring 自动回退到 NoOpCacheManager，缓存才真正关闭
 * （用于压测"无缓存"对照场景）。
 */
@Configuration
public class RedisConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.cache", name = "type",
            havingValue = "redis", matchIfMissing = true)
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // key 用字符串，value 用 JSON
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                // 缓存 10 分钟自动过期，避免脏数据长期驻留
                .entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }
}
