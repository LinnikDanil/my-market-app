package ru.practicum.market.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.market.domain.exception.OrderConflictException;
import ru.practicum.market.domain.exception.OrderNotFoundException;
import ru.practicum.market.domain.model.Order;
import ru.practicum.market.domain.model.OrderItem;
import ru.practicum.market.repository.CartItemRepository;
import ru.practicum.market.repository.OrderItemRepository;
import ru.practicum.market.repository.OrderRepository;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.market.web.dto.OrderResponseDto;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Nested
    @DisplayName("getOrders")
    class getOrders {

        @Test
        @DisplayName("ok")
        void test1() {
            var orders = createOrders();
            var firstOrder = orders.getFirst();

            doReturn(orders).when(orderRepository).findAllFetch();

            var response = orderService.getOrders();

            assertThat(response)
                    .isNotNull()
                    .hasSize(orders.size());

            var firstResponse = response.getFirst();
            assertOrderResponse(firstResponse, firstOrder);

            verify(orderRepository, times(1)).findAllFetch();
        }
    }

    @Nested
    @DisplayName("getOrder")
    class getOrder {

        @Test
        @DisplayName("ok")
        void test1() {
            var order = createOrders().getFirst();
            doReturn(Optional.of(order)).when(orderRepository).findByIdFetch(order.getId());

            var response = orderService.getOrder(order.getId());

            assertOrderResponse(response, order);
            verify(orderRepository, times(1)).findByIdFetch(order.getId());
        }

        @Test
        @DisplayName("not found")
        void test2() {
            var orderId = 99L;

            doReturn(Optional.empty()).when(orderRepository).findByIdFetch(orderId);

            assertThatExceptionOfType(OrderNotFoundException.class)
                    .isThrownBy(() -> orderService.getOrder(orderId))
                    .withMessage("Order with id = %d not found.".formatted(orderId));

            verify(orderRepository, times(1)).findByIdFetch(orderId);
        }
    }

    @Nested
    @DisplayName("createOrder")
    class createOrder {

        @Test
        @DisplayName("ok")
        void test1() {
            var cartItems = TestDataFactory.createCartItemsForSave(3);
            var totalSum = cartItems.stream()
                    .map(ci -> ci.getItem().getPrice() * ci.getQuantity())
                    .reduce(0L, Long::sum);

            doReturn(cartItems).when(cartItemRepository).findAllFetch();
            doReturn(createOrderWithId(totalSum)).when(orderRepository).save(any(Order.class));

            var orderId = orderService.createOrder();

            assertThat(orderId).isEqualTo(1L);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, times(1)).save(orderCaptor.capture());

            var savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getTotalSum()).isEqualTo(totalSum);

            ArgumentCaptor<Iterable<OrderItem>> orderItemsCaptor = ArgumentCaptor.forClass(Iterable.class);
            verify(orderItemRepository, times(1)).saveAll(orderItemsCaptor.capture());

            var savedOrderItems = orderItemsCaptor.getValue();
            assertThat(savedOrderItems)
                    .hasSize(cartItems.size())
                    .allSatisfy(orderItem -> {
                        var cartItem = cartItems.stream()
                                .filter(ci -> ci.getItem().equals(orderItem.getItem()))
                                .findFirst()
                                .orElseThrow();
                        assertThat(orderItem.getOrder().getId()).isEqualTo(1L);
                        assertThat(orderItem.getItem().getTitle()).isEqualTo(cartItem.getItem().getTitle());
                        assertThat(orderItem.getQuantity()).isEqualTo(cartItem.getQuantity());
                        assertThat(orderItem.getPriceAtOrder()).isEqualTo(cartItem.getItem().getPrice());
                    });

            verify(cartItemRepository, times(1)).deleteAll();
        }

        @Test
        @DisplayName("cart empty")
        void test2() {
            doReturn(List.of()).when(cartItemRepository).findAllFetch();

            assertThatExceptionOfType(OrderConflictException.class)
                    .isThrownBy(orderService::createOrder)
                    .withMessage("Order must contain at least one item.");

            verify(orderRepository, never()).save(any());
            verify(orderItemRepository, never()).saveAll(any());
            verify(cartItemRepository, never()).deleteAll();
        }
    }

    private static List<Order> createOrders() {
        var order = TestDataFactory.createOrder(1L, 200L);
        var items = TestDataFactory.createItems(2);
        var orderItems = TestDataFactory.createOrderItems(order, items);
        order.setOrderItems(orderItems);
        return List.of(order);
    }

    private static Order createOrderWithId(long totalSum) {
        var order = TestDataFactory.createOrder(totalSum);
        order.setId(1L);
        return order;
    }

    private static void assertOrderResponse(OrderResponseDto response, Order order) {
        assertThat(response.id()).isEqualTo(order.getId());
        assertThat(response.totalSum()).isEqualTo(order.getTotalSum());
        assertThat(response.items())
                .hasSize(order.getOrderItems().size())
                .allSatisfy(item -> {
                    var orderItem = order.getOrderItems().stream()
                            .filter(oi -> oi.getItem().getId() == item.id())
                            .findFirst()
                            .orElseThrow();
                    assertThat(item.title()).isEqualTo(orderItem.getItem().getTitle());
                    assertThat(item.description()).isEqualTo(orderItem.getItem().getDescription());
                    assertThat(item.imgPath()).isEqualTo(orderItem.getItem().getImgPath());
                    assertThat(item.price()).isEqualTo(orderItem.getPriceAtOrder());
                    assertThat(item.count()).isEqualTo(orderItem.getQuantity());
                });
    }
}
