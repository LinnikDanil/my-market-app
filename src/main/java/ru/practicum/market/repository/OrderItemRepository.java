package ru.practicum.market.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.market.domain.model.OrderItem;

@Repository
public interface OrderItemRepository extends CrudRepository<OrderItem, Long> {
}
