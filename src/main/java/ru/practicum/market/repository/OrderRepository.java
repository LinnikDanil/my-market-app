package ru.practicum.market.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.market.domain.model.Order;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT o
            FROM Order o
            LEFT JOIN FETCH o.orderItems
            """)
    List<Order> findAllFetch();

    @Query("""
            SELECT o
            FROM Order o
            LEFT JOIN FETCH o.orderItems
            WHERE o.id = :id
            """)
    Optional<Order> findByIdFetch(long id);
}
