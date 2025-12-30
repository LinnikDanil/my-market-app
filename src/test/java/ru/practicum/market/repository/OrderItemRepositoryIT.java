package ru.practicum.market.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.market.domain.model.OrderItem;
import ru.practicum.market.util.PostgresContainer;
import ru.practicum.market.util.TestDataFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Testcontainers
@ImportTestcontainers(PostgresContainer.class)
@DisplayName("OrderItemRepository")
class OrderItemRepositoryIT {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private DatabaseClient db;

    @BeforeEach
    void clean() {
        db.sql("TRUNCATE TABLE order_items, orders, items RESTART IDENTITY CASCADE").then().block();
    }

    @Test
    @DisplayName("save and findByOrderId")
    void test1() {
        var item = TestDataFactory.createItemForSave(1);
        var order = TestDataFactory.createOrder(500L);

        var resultMono = itemRepository.save(item)
                .flatMap(savedItem -> orderRepository.save(order)
                        .flatMap(savedOrder -> {
                            var orderItem = new OrderItem(savedOrder.getId(), savedItem.getId(), 4, savedItem.getPrice());
                            return orderItemRepository.save(orderItem)
                                    .thenMany(orderItemRepository.findByOrderId(savedOrder.getId()))
                                    .collectList();
                        })
                );

        var result = resultMono.block();
        assertThat(result).hasSize(1);
        var entity = result.getFirst();
        assertThat(entity.getId()).isGreaterThan(0);
        assertThat(entity.getQuantity()).isEqualTo(4);
        assertThat(entity.getPriceAtOrder()).isEqualTo(100L);
    }

    @Test
    @DisplayName("findByOrderIdIn")
    void test2() {
        var items = TestDataFactory.createItemsForSave(2);
        var order1 = TestDataFactory.createOrder(300L);
        var order2 = TestDataFactory.createOrder(600L);

        var resultMono = itemRepository.saveAll(items)
                .collectList()
                .flatMap(savedItems -> orderRepository.saveAll(List.of(order1, order2))
                        .collectList()
                        .flatMap(savedOrders -> {
                            var orderItems = List.of(
                                    new OrderItem(savedOrders.get(0).getId(), savedItems.get(0).getId(), 2,
                                            savedItems.get(0).getPrice()),
                                    new OrderItem(savedOrders.get(1).getId(), savedItems.get(1).getId(), 3,
                                            savedItems.get(1).getPrice())
                            );

                            return orderItemRepository.saveAll(orderItems)
                                    .thenMany(orderItemRepository.findByOrderIdIn(savedOrders.stream()
                                            .map(order -> order.getId())
                                            .toList()))
                                    .collectList();
                        })
                );

        var result = resultMono.block();
        assertThat(result).hasSize(2);
        assertThat(result)
                .allSatisfy(orderItem -> {
                    assertThat(orderItem.getId()).isGreaterThan(0);
                    assertThat(orderItem.getOrderId()).isPositive();
                    assertThat(orderItem.getItemId()).isPositive();
                    assertThat(orderItem.getQuantity()).isPositive();
                });
    }
}
