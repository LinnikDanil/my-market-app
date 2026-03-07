package ru.practicum.market.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import ru.practicum.market.repository.CartItemRepository;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.repository.OrderItemRepository;
import ru.practicum.market.repository.OrderRepository;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.payments.integration.domain.HoldRq;
import ru.practicum.payments.integration.domain.HoldRs;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl")
class OrderServiceImplTest {

    private static final long USER_ID = TestDataFactory.USER_ID;

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
    class GetOrders {

        @Test
        @DisplayName("ok")
        void test1() {
            var items = TestDataFactory.createItems(1);
            var orders = List.of(TestDataFactory.createOrder(1L, 500L));
            var orderItems = List.of(new OrderItem(1L, items.getFirst().getId(), 2, items.getFirst().getPrice()));

            when(orderRepository.findByUserId(USER_ID)).thenReturn(Flux.fromIterable(orders));
            when(orderItemRepository.findByOrderIdIn(List.of(1L))).thenReturn(Flux.fromIterable(orderItems));
            when(itemRepository.findByIdIn(List.of(items.getFirst().getId()))).thenReturn(Flux.fromIterable(items));

            var result = orderService.getOrders(USER_ID).collectList().block();
            assertThat(result).hasSize(1);
            var response = result.getFirst();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.totalSum()).isEqualTo(500L);
            assertThat(response.items()).hasSize(1);

            verify(orderRepository, times(1)).findByUserId(USER_ID);
        }

        @Test
        @DisplayName("empty")
        void test2() {
            when(orderRepository.findByUserId(USER_ID)).thenReturn(Flux.empty());

            var result = orderService.getOrders(USER_ID).collectList().block();

            assertThat(result).isEmpty();
            verify(orderItemRepository, never()).findByOrderIdIn(anyCollection());
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        @DisplayName("ok")
        void test1() {
            var items = TestDataFactory.createItems(1);
            var order = TestDataFactory.createOrder(1L, 400L);
            var orderItems = List.of(new OrderItem(order.getId(), items.getFirst().getId(), 2, items.getFirst().getPrice()));

            when(orderRepository.findByUserIdAndId(USER_ID, order.getId())).thenReturn(Mono.just(order));
            when(orderItemRepository.findByOrderId(order.getId())).thenReturn(Flux.fromIterable(orderItems));
            when(itemRepository.findByIdIn(List.of(items.getFirst().getId())))
                    .thenReturn(Flux.fromIterable(items));

            var response = orderService.getOrder(USER_ID, order.getId()).block();
            assertThat(response.id()).isEqualTo(order.getId());
            assertThat(response.totalSum()).isEqualTo(order.getTotalSum());
            assertThat(response.items()).hasSize(1);
        }

        @Test
        @DisplayName("not found")
        void test2() {
            var orderId = 99L;

            when(orderRepository.findByUserIdAndId(USER_ID, orderId)).thenReturn(Mono.empty());

            assertThatExceptionOfType(OrderNotFoundException.class)
                    .isThrownBy(() -> orderService.getOrder(USER_ID, orderId).block());

            verify(orderItemRepository, never()).findByOrderId(anyLong());
        }
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("ok")
        void test1() {
            var items = TestDataFactory.createItems(2);
            var firstCartItem = TestDataFactory.createCartItem(USER_ID, items.get(0).getId(), 2);
            firstCartItem.setId(10L);
            var secondCartItem = TestDataFactory.createCartItem(USER_ID, items.get(1).getId(), 1);
            secondCartItem.setId(11L);
            var cartItems = List.of(firstCartItem, secondCartItem);

            var orderId = 5L;
            var holdPaymentId = UUID.randomUUID();
            var holdRs = new HoldRs().paymentId(holdPaymentId);

            var orderItems = List.of(
                    new OrderItem(orderId, items.get(0).getId(), 2, items.get(0).getPrice()),
                    new OrderItem(orderId, items.get(1).getId(), 1, items.get(1).getPrice())
            );

            when(cartItemRepository.findByUserId(USER_ID)).thenReturn(Flux.fromIterable(cartItems));
            when(itemRepository.findByIdIn(List.of(items.get(0).getId(), items.get(1).getId())))
                    .thenReturn(Flux.fromIterable(items));
            when(paymentAdapter.hold(eq(USER_ID), any(HoldRq.class))).thenReturn(Mono.just(holdRs));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(orderId);
                return Mono.just(order);
            });
            when(orderItemRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(orderItems));
            when(cartItemRepository.deleteByIdIn(List.of(10L, 11L))).thenReturn(Mono.empty());
            when(paymentAdapter.confirm(holdPaymentId)).thenReturn(Mono.empty());

            var response = orderService.createOrder(USER_ID).block();
            assertThat(response).isEqualTo(orderId);

            ArgumentCaptor<HoldRq> holdRqCaptor = ArgumentCaptor.forClass(HoldRq.class);
            verify(paymentAdapter).hold(eq(USER_ID), holdRqCaptor.capture());
            assertThat(holdRqCaptor.getValue().getAmount()).isEqualTo(BigDecimal.valueOf(400));
            verify(cartItemRepository, times(1)).deleteByIdIn(List.of(10L, 11L));
        }

        @Test
        @DisplayName("empty cart")
        void test2() {
            when(cartItemRepository.findByUserId(USER_ID)).thenReturn(Flux.empty());

            assertThatExceptionOfType(OrderConflictException.class)
                    .isThrownBy(() -> orderService.createOrder(USER_ID).block());

            verify(orderRepository, never()).save(any());
            verify(paymentAdapter, never()).hold(anyLong(), any());
        }

        @Test
        @DisplayName("db failure triggers payment cancel")
        void test3() {
            var item = TestDataFactory.createItem(1L);
            var cartItem = TestDataFactory.createCartItem(USER_ID, item.getId(), 2);
            cartItem.setId(101L);
            var paymentId = UUID.randomUUID();

            when(cartItemRepository.findByUserId(USER_ID)).thenReturn(Flux.just(cartItem));
            when(itemRepository.findByIdIn(List.of(item.getId()))).thenReturn(Flux.just(item));
            when(paymentAdapter.hold(eq(USER_ID), any(HoldRq.class))).thenReturn(Mono.just(new HoldRs().paymentId(paymentId)));
            when(orderRepository.save(any(Order.class))).thenReturn(Mono.error(new IllegalStateException("db fail")));
            when(paymentAdapter.cancel(paymentId)).thenReturn(Mono.empty());

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> orderService.createOrder(USER_ID).block())
                    .withMessageContaining("db fail");

            verify(paymentAdapter).cancel(paymentId);
        }
    }
}
