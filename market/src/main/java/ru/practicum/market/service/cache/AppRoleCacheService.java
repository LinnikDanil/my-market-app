package ru.practicum.market.service.cache;

import reactor.core.publisher.Mono;

/**
 * Кэш-сервис для поиска идентификаторов ролей по имени.
 */
public interface AppRoleCacheService {
    /**
     * Возвращает идентификатор роли по её имени.
     *
     * @param name имя роли (например, `ROLE_USER`)
     * @return идентификатор роли
     */
    Mono<Long> getRoleIdByName(String name);
}
