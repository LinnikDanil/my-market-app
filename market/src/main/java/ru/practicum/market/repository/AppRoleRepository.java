package ru.practicum.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.model.AppRole;

import java.util.List;

@Repository
public interface AppRoleRepository extends ReactiveCrudRepository<AppRole, Long> {
    Flux<AppRole> findByIdIn(List<Long> rolesId);

    Mono<AppRole> findByName(String name);
}
