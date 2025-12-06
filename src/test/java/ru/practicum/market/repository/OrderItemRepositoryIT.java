package ru.practicum.market.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.market.domain.model.OrderItem;
import ru.practicum.market.util.PostgresContainer;
import ru.practicum.market.util.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
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

    @Test
    @DisplayName("save and find")
    void test1() {
        var item = itemRepository.save(TestDataFactory.createItemForSave(1));
        var order = orderRepository.save(TestDataFactory.createOrder(500L));
        var orderItem = OrderItem.builder()
                .order(order)
                .item(item)
                .quantity(4)
                .priceAtOrder(item.getPrice())
                .build();

        var savedOrderItem = orderItemRepository.save(orderItem);

        var result = orderItemRepository.findById(savedOrderItem.getId());

        assertThat(result).isPresent();
        var entity = result.get();
        assertThat(entity.getId()).isGreaterThan(0);
        assertThat(entity.getOrder().getId()).isEqualTo(order.getId());
        assertThat(entity.getItem().getId()).isEqualTo(item.getId());
        assertThat(entity.getQuantity()).isEqualTo(orderItem.getQuantity());
        assertThat(entity.getPriceAtOrder()).isEqualTo(orderItem.getPriceAtOrder());
    }
}
