package ru.practicum.market.service.cache;

import reactor.core.publisher.Mono;

public interface AppRoleCacheService {
    Mono<Long> getRoleIdByName(String name);
}
