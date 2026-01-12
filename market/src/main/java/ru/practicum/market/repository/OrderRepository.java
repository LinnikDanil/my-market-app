package ru.practicum.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.market.domain.model.Order;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
}
