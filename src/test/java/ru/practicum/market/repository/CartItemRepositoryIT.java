package ru.practicum.market.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.util.PostgresContainer;
import ru.practicum.market.util.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ImportTestcontainers(PostgresContainer.class)
@DisplayName("CartItemRepository")
class CartItemRepositoryIT {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    @DisplayName("findByItemId")
    void test1() {
        var item = itemRepository.save(TestDataFactory.createItemForSave(1));
        var cartItem = new CartItem(item, 3);
        cartItemRepository.save(cartItem);

        var result = cartItemRepository.findByItemId(item.getId());

        assertThat(result).isPresent();
        var resultCartItem = result.get();
        assertThat(resultCartItem.getId()).isGreaterThan(0);
        assertThat(resultCartItem.getQuantity()).isEqualTo(cartItem.getQuantity());
        assertThat(resultCartItem.getItem().getId()).isEqualTo(item.getId());
        assertThat(resultCartItem.getItem().getTitle()).isEqualTo(item.getTitle());
        assertThat(resultCartItem.getItem().getDescription()).isEqualTo(item.getDescription());
        assertThat(resultCartItem.getItem().getImgPath()).isEqualTo(item.getImgPath());
        assertThat(resultCartItem.getItem().getPrice()).isEqualTo(item.getPrice());
    }

    @Test
    @DisplayName("findByItemIds")
    void test2() {
        var items = itemRepository.saveAll(TestDataFactory.createItemsForSave(2));
        var cartItems = cartItemRepository.saveAll(items.stream()
                .map(item -> new CartItem(item, 2))
                .toList());
        var itemIds = items.stream().map(Item::getId).toList();

        var result = cartItemRepository.findByItemIds(itemIds);

        assertThat(result)
                .isNotEmpty()
                .hasSize(cartItems.size());
        assertThat(result)
                .allSatisfy(ci -> {
                    var sourceItem = items.stream()
                            .filter(it -> it.getId() == ci.getItem().getId())
                            .findFirst()
                            .orElseThrow();
                    assertThat(ci.getId()).isGreaterThan(0);
                    assertThat(ci.getQuantity()).isEqualTo(2);
                    assertThat(ci.getItem().getTitle()).isEqualTo(sourceItem.getTitle());
                    assertThat(ci.getItem().getPrice()).isEqualTo(sourceItem.getPrice());
                });
    }

    @Test
    @DisplayName("findAllFetch")
    void test3() {
        var items = itemRepository.saveAll(TestDataFactory.createItemsForSave(3));
        cartItemRepository.saveAll(items.stream()
                .map(item -> new CartItem(item, 5))
                .toList());

        var result = cartItemRepository.findAllFetch();

        assertThat(result)
                .isNotEmpty()
                .hasSize(items.size());
        assertThat(result)
                .allSatisfy(ci -> {
                    var item = items.stream()
                            .filter(it -> it.getId() == ci.getItem().getId())
                            .findFirst()
                            .orElseThrow();
                    assertThat(ci.getId()).isGreaterThan(0);
                    assertThat(ci.getQuantity()).isEqualTo(5);
                    assertThat(ci.getItem().getTitle()).isEqualTo(item.getTitle());
                    assertThat(ci.getItem().getDescription()).isEqualTo(item.getDescription());
                    assertThat(ci.getItem().getImgPath()).isEqualTo(item.getImgPath());
                    assertThat(ci.getItem().getPrice()).isEqualTo(item.getPrice());
                });
    }
}
