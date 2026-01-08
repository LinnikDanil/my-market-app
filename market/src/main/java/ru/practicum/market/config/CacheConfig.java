package ru.practicum.market.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import ru.practicum.market.service.cache.dto.CartCacheDto;
import ru.practicum.market.service.cache.dto.ItemCacheDto;
import ru.practicum.market.service.cache.dto.ItemsPageCacheDto;

import java.time.Duration;
import java.util.Map;

@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            @Value("${spring.cache.redis.time-to-live}") Duration ttl) {

        var itemCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new Jackson2JsonRedisSerializer<>(ItemCacheDto.class)
                        )
                );

        var itemsPageCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new Jackson2JsonRedisSerializer<>(ItemsPageCacheDto.class)
                        )
                );

        var cartCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new Jackson2JsonRedisSerializer<>(CartCacheDto.class)
                        )
                );

        return builder -> builder.withInitialCacheConfigurations(
                Map.of(
                        "item", itemCacheConfiguration,
                        "items-page", itemsPageCacheConfiguration,
                        "cart", cartCacheConfiguration
                        )
        );
    }
}
