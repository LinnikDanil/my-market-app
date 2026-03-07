package ru.practicum.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.model.AppUser;

/**
 * Реактивный репозиторий пользователей приложения.
 */
@Repository
public interface AppUserRepository extends ReactiveCrudRepository<AppUser, Long> {
    /**
     * Возвращает пользователя по username.
     */
    Mono<AppUser> findByUsername(String username);
}
