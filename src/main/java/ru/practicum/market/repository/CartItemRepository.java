package ru.practicum.market.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.market.domain.model.CartItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("""
            SELECT ci
            FROM CartItem ci
            JOIN FETCH ci.item i
            WHERE i.id = :itemId
            """)
    Optional<CartItem> findByItemId(long itemId);

    @Query("""
            SELECT ci
            FROM CartItem ci
            JOIN FETCH ci.item i
            WHERE i.id IN (:itemIds)
            """)
    List<CartItem> findByItemIds(List<Long> itemIds);

    @Query("""
            SELECT ci
            FROM CartItem ci
            JOIN FETCH ci.item i
            """)
    List<CartItem> findAllFetch();

}
