package ru.practicum.market.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.market.domain.model.Order;
import ru.practicum.market.domain.model.OrderItem;
import ru.practicum.market.util.PostgresContainer;
import ru.practicum.market.util.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ImportTestcontainers(PostgresContainer.class)
@DisplayName("OrderRepository")
class OrderRepositoryIT {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    @DisplayName("findAllFetch")
    void test1() {
        var savedOrder = orderRepository.save(createOrderWithItems());

        var result = orderRepository.findAllFetch();

        assertThat(result)
                .isNotNull()
                .hasSize(1);

        var firstOrder = result.getFirst();
        assertOrder(firstOrder, savedOrder);
    }

    @Test
    @DisplayName("findByIdFetch")
    void test2() {
        var savedOrder = orderRepository.save(createOrderWithItems());

        var result = orderRepository.findByIdFetch(savedOrder.getId());

        assertThat(result).isPresent();
        assertOrder(result.get(), savedOrder);
    }

    private Order createOrderWithItems() {
        var items = itemRepository.saveAll(TestDataFactory.createItemsForSave(2));
        var order = TestDataFactory.createOrder(300L);
        var orderItems = items.stream()
                .map(item -> OrderItem.builder()
                        .order(order)
                        .item(item)
                        .quantity(2)
                        .priceAtOrder(item.getPrice())
                        .build())
                .toList();
        order.setOrderItems(orderItems);
        return order;
    }

    private static void assertOrder(Order result, Order expected) {
        assertThat(result.getId()).isEqualTo(expected.getId());
        assertThat(result.getTotalSum()).isEqualTo(expected.getTotalSum());
        assertThat(result.getOrderItems())
                .isNotEmpty()
                .hasSize(expected.getOrderItems().size())
                .allSatisfy(orderItem -> {
                    var sourceItem = expected.getOrderItems().stream()
                            .filter(oi -> oi.getItem().getId() == orderItem.getItem().getId())
                            .findFirst()
                            .orElseThrow();
                    assertThat(orderItem.getOrder().getId()).isEqualTo(expected.getId());
                    assertThat(orderItem.getQuantity()).isEqualTo(sourceItem.getQuantity());
                    assertThat(orderItem.getPriceAtOrder()).isEqualTo(sourceItem.getPriceAtOrder());
                });
    }
}
