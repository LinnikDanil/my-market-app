package ru.practicum.market.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.OrderConflictException;
import ru.practicum.market.domain.exception.OrderNotFoundException;
import ru.practicum.market.domain.model.Order;
import ru.practicum.market.domain.model.OrderItem;
import ru.practicum.market.integration.PaymentAdapter;
import ru.practicum.market.integration.dto.HoldRq;
import ru.practicum.market.integration.dto.HoldRs;
import ru.practicum.market.repository.CartItemRepository;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.repository.OrderItemRepository;
import ru.practicum.market.repository.OrderRepository;
import ru.practicum.market.util.TestDataFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private PaymentAdapter paymentAdapter;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Nested
    @DisplayName("getOrders")
    class getOrders {

        @Test
        @DisplayName("ok")
        void test1() {
            var items = TestDataFactory.createItems(2);
            var orders = List.of(TestDataFactory.createOrder(1L, 500L));
            var orderItems = List.of(new OrderItem(1L, items.get(0).getId(), 2, items.get(0).getPrice()));

            when(orderRepository.findAll()).thenReturn(Flux.fromIterable(orders));
            when(orderItemRepository.findByOrderIdIn(List.of(1L))).thenReturn(Flux.fromIterable(orderItems));
            when(itemRepository.findByIdIn(List.of(items.get(0).getId()))).thenReturn(Flux.fromIterable(items));

            var result = orderService.getOrders().collectList().block();
            assertThat(result).hasSize(1);
            var response = result.getFirst();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.totalSum()).isEqualTo(500L);
            assertThat(response.items()).hasSize(1);

            verify(orderRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("getOrder")
    class getOrder {

        @Test
        @DisplayName("ok")
        void test1() {
            var items = TestDataFactory.createItems(1);
            var order = TestDataFactory.createOrder(1L, 400L);
            var orderItems = List.of(new OrderItem(order.getId(), items.getFirst().getId(), 2, items.getFirst().getPrice()));

            when(orderRepository.findById(order.getId())).thenReturn(Mono.just(order));
            when(orderItemRepository.findByOrderId(order.getId())).thenReturn(Flux.fromIterable(orderItems));
            when(itemRepository.findByIdIn(List.of(items.getFirst().getId())))
                    .thenReturn(Flux.fromIterable(items));

            var response = orderService.getOrder(order.getId()).block();
            assertThat(response.id()).isEqualTo(order.getId());
            assertThat(response.totalSum()).isEqualTo(order.getTotalSum());
            assertThat(response.items()).hasSize(1);
        }

        @Test
        @DisplayName("not found")
        void test2() {
            var orderId = 99L;

            when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

            assertThatExceptionOfType(OrderNotFoundException.class)
                    .isThrownBy(() -> orderService.getOrder(orderId).block());

            verify(orderItemRepository, never()).findByOrderId(anyLong());
        }
    }

    @Nested
    @DisplayName("createOrder")
    class createOrder {

        @Test
        @DisplayName("ok")
        void test1() {
            var items = TestDataFactory.createItems(2);
            var cartItems = List.of(
                    TestDataFactory.createCartItem(items.get(0).getId(), 2),
                    TestDataFactory.createCartItem(items.get(1).getId(), 1)
            );
            var totalSum = 400L;
            var order = new Order(totalSum);
            order.setId(5L);
            var orderItems = List.of(
                    new OrderItem(order.getId(), items.get(0).getId(), 2, items.get(0).getPrice()),
                    new OrderItem(order.getId(), items.get(1).getId(), 1, items.get(1).getPrice())
            );
            var holdRq = new HoldRq(BigDecimal.valueOf(totalSum));
            var holdRs = new HoldRs(UUID.randomUUID());

            when(cartItemRepository.findAll()).thenReturn(Flux.fromIterable(cartItems));
            when(itemRepository.findByIdIn(List.of(items.get(0).getId(), items.get(1).getId())))
                    .thenReturn(Flux.fromIterable(items));
            when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));
            when(orderItemRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(orderItems));
            when(cartItemRepository.deleteAll()).thenReturn(Mono.empty());
            when(paymentAdapter.hold(holdRq)).thenReturn(Mono.just(holdRs));
            when(paymentAdapter.confirm(holdRs.paymentId())).thenReturn(Mono.empty());

            var response = orderService.createOrder().block();
            assertThat(response).isEqualTo(order.getId());

            verify(cartItemRepository, times(1)).deleteAll();
        }

        @Test
        @DisplayName("empty cart")
        void test2() {
            when(cartItemRepository.findAll()).thenReturn(Flux.empty());

            assertThatExceptionOfType(OrderConflictException.class)
                    .isThrownBy(() -> orderService.createOrder().block());

            verify(orderRepository, never()).save(any());
        }
    }
}
