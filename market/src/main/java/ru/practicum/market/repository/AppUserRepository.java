package ru.practicum.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.model.AppUser;

@Repository
public interface AppUserRepository extends ReactiveCrudRepository<AppUser, Long> {
    Mono<AppUser> findByUsername(String username);
}
