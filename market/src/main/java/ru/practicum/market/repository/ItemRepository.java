package ru.practicum.market.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.model.Item;

import java.util.Collection;

/**
 * Реактивный репозиторий товаров.
 */
@Repository
public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {

    /**
     * Ищет товары по подстроке в названии или описании с пагинацией.
     */
    Flux<Item> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title,
            String description,
            Pageable pageable
    );

    /**
     * Считает количество товаров по строке поиска в названии/описании.
     */
    Mono<Long> countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    /**
     * Возвращает все товары с пагинацией.
     */
    Flux<Item> findAllBy(Pageable pageable);

    /**
     * Возвращает товары по набору идентификаторов.
     */
    Flux<Item> findByIdIn(Collection<Long> ids);
}
