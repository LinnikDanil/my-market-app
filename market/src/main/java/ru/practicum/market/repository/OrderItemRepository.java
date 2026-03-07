package ru.practicum.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.practicum.market.domain.model.OrderItem;

import java.util.Collection;

/**
 * Реактивный репозиторий позиций заказа.
 */
@Repository
public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, Long> {
    /**
     * Возвращает позиции для набора заказов.
     */
    Flux<OrderItem> findByOrderIdIn(Collection<Long> orderIds);

    /**
     * Возвращает позиции конкретного заказа.
     */
    Flux<OrderItem> findByOrderId(long id);
}
