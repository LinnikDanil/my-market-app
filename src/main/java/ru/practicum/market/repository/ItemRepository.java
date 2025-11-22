package ru.practicum.market.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.market.domain.model.Item;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title,
            String description,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Item i set i.count = i.count + 1 where i.id = :id")
    int incrementItemCount(long id);

    @Modifying
    @Query("UPDATE Item i set i.count = i.count - 1 where i.id = :id")
    int decrementItemCount(long id);

    List<Item> findByCountGreaterThan(int countIsGreaterThan);
}
