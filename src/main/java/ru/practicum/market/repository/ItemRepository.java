package ru.practicum.market.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.model.Item;

public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {

    Flux<Item> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title,
            String description,
            Pageable pageable
    );

    Mono<Long> countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    Flux<Item> findAll(Pageable pageable);
}
