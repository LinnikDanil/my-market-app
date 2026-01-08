package ru.practicum.market.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.market.util.PostgresContainer;
import ru.practicum.market.util.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Testcontainers
@ImportTestcontainers(PostgresContainer.class)
@DisplayName("OrderRepository")
class OrderRepositoryIT {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DatabaseClient db;

    @BeforeEach
    void clean() {
        db.sql("TRUNCATE TABLE order_items, orders RESTART IDENTITY CASCADE").then().block();
    }

    @Test
    @DisplayName("save and findAll")
    void test1() {
        var order = TestDataFactory.createOrder(500L);

        var resultMono = orderRepository.save(order)
                .thenMany(orderRepository.findAll())
                .collectList();

        var result = resultMono.block();
        assertThat(result).hasSize(1);
        var savedOrder = result.getFirst();
        assertThat(savedOrder.getId()).isGreaterThan(0);
        assertThat(savedOrder.getTotalSum()).isEqualTo(order.getTotalSum());
    }

    @Test
    @DisplayName("save and findById")
    void test2() {
        var order = TestDataFactory.createOrder(700L);

        var resultMono = orderRepository.save(order)
                .flatMap(saved -> orderRepository.findById(saved.getId()));

        var result = resultMono.block();
        assertThat(result.getId()).isGreaterThan(0);
        assertThat(result.getTotalSum()).isEqualTo(order.getTotalSum());
    }
}
