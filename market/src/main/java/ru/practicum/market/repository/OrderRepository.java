package ru.practicum.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.model.Order;

/**
 * Реактивный репозиторий заказов.
 */
@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
    /**
     * Возвращает все заказы пользователя.
     */
    Flux<Order> findByUserId(Long userId);

    /**
     * Возвращает заказ пользователя по идентификатору заказа.
     */
    Mono<Order> findByUserIdAndId(long userId, long orderId);
}
