package ru.practicum.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.model.CartItem;

import java.util.Collection;
import java.util.List;

/**
 * Реактивный репозиторий позиций корзины.
 */
@Repository
public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

    /**
     * Ищет позицию корзины пользователя по идентификатору товара.
     */
    Mono<CartItem> findByUserIdAndItemId(long userId, long itemId);

    /**
     * Возвращает позиции корзин по списку идентификаторов товаров.
     */
    Flux<CartItem> findByItemIdIn(List<Long> itemIds);

    /**
     * Возвращает все позиции корзины пользователя.
     */
    Flux<CartItem> findByUserId(long userId);

    /**
     * Удаляет позиции корзины по списку идентификаторов.
     */
    Mono<Void> deleteByIdIn(Collection<Long> ids);

    /**
     * Возвращает позиции корзины пользователя для заданного списка товаров.
     */
    Flux<CartItem> findByUserIdAndItemIdIn(long userId, List<Long> itemIds);
}
