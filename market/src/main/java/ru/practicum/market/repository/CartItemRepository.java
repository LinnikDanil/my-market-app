package ru.practicum.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.model.CartItem;

import java.util.Collection;
import java.util.List;

@Repository
public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

    Mono<CartItem> findByUserIdAndItemId(long userId, long itemId);

    Flux<CartItem> findByItemIdIn(List<Long> itemIds);

    Flux<CartItem> findByUserId(long userId);

    Mono<Void> deleteByIdIn(Collection<Long> ids);

    Flux<CartItem> findByUserIdAndItemIdIn(long userId, List<Long> itemIds);
}
