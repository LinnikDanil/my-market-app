package ru.practicum.market.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.util.PostgresContainer;
import ru.practicum.market.util.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Testcontainers
@ImportTestcontainers(PostgresContainer.class)
@DisplayName("CartItemRepository")
class CartItemRepositoryIT {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private DatabaseClient db;

    @BeforeEach
    void clean() {
        db.sql("TRUNCATE TABLE cart_items, items RESTART IDENTITY CASCADE").then().block();
    }

    @Test
    @DisplayName("findByItemId")
    void test1() {
        var item = TestDataFactory.createItemForSave(1);
        var quantity = 3;

        var resultMono = itemRepository.save(item)
                .flatMap(savedItem -> {
                    var cartItem = new CartItem();
                    cartItem.setItemId(savedItem.getId());
                    cartItem.setQuantity(quantity);

                    return cartItemRepository.save(cartItem)
                            .then(cartItemRepository.findByItemId(savedItem.getId()));
                });

        var result = resultMono.block();
        assertThat(result.getId()).isGreaterThan(0);
        assertThat(result.getItemId()).isGreaterThan(0);
        assertThat(result.getQuantity()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("findByItemIds")
    void test2() {
        var items = TestDataFactory.createItemsForSave(2);
        var quantity = 2;

        var resultMono = itemRepository.saveAll(items)
                .collectList()
                .flatMap(savedItems -> {
                    var cartItems = savedItems.stream()
                            .map(item -> TestDataFactory.createCartItem(item.getId(), quantity))
                            .toList();
                    var itemIds = savedItems.stream().map(item -> item.getId()).toList();

                    return cartItemRepository.saveAll(cartItems)
                            .thenMany(cartItemRepository.findByItemIdIn(itemIds))
                            .collectList();
                });

        var result = resultMono.block();
        assertThat(result)
                .isNotEmpty()
                .hasSize(items.size());
        assertThat(result)
                .allSatisfy(ci -> {
                    assertThat(ci.getId()).isGreaterThan(0);
                    assertThat(ci.getItemId()).isPositive();
                    assertThat(ci.getQuantity()).isEqualTo(quantity);
                });
    }
}
