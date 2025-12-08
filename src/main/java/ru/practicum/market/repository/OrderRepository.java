package ru.practicum.market.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.market.domain.model.Order;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    @Query("SELECT o FROM Order o")
    List<Order> findAllFetch();

    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN FETCH o.orderItems oi
            JOIN FETCH oi.item
            WHERE o.id = :id
            """)
    Optional<Order> findByIdFetch(long id);
}
