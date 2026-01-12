package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.OrderConflictException;
import ru.practicum.market.domain.exception.OrderNotFoundException;
import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.domain.model.Order;
import ru.practicum.market.domain.model.OrderItem;
import ru.practicum.market.integration.PaymentAdapter;
import ru.practicum.market.integration.dto.HoldRq;
import ru.practicum.market.repository.CartItemRepository;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.repository.OrderItemRepository;
import ru.practicum.market.repository.OrderRepository;
import ru.practicum.market.service.OrderService;
import ru.practicum.market.web.dto.OrderResponseDto;
import ru.practicum.market.web.mapper.OrderMapper;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final PaymentAdapter paymentAdapter;

    @Override
    @Transactional(readOnly = true)
    public Flux<OrderResponseDto> getOrders() {
        log.debug("Request to fetch all orders");
        // Все заказы
        return orderRepository.findAll().collectList()
                .flatMapMany(orders -> {
                    if (orders.isEmpty()) return Flux.empty();
                    var orderIds = orders.stream().map(Order::getId).toList();

                    // Все orderItem, которые использовались в заказах
                    return orderItemRepository.findByOrderIdIn(orderIds)
                            .collectList()
                            .flatMapMany(orderItems -> {
                                var itemIds = orderItems.stream().map(OrderItem::getItemId).toList();

                                // Все товары, которые использовались в заказах
                                return itemRepository.findByIdIn(itemIds).collectList()
                                        .flatMapMany(items -> {
                                            var responses = OrderMapper.toOrderResponseDtos(orders, orderItems, items);
                                            log.debug("Fetched {} orders", orders.size());

                                            return Flux.fromIterable(responses);
                                        });
                            });
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<OrderResponseDto> getOrder(long id) {
        log.debug("Request to fetch order with id={}", id);

        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(id, "Order with id = %d not found.".formatted(id))))
                .flatMap(order -> orderItemRepository.findByOrderId(id).collectList()
                        .flatMap(orderItems -> {
                            var itemIds = orderItems.stream().map(OrderItem::getItemId).toList();
                            return itemRepository.findByIdIn(itemIds).collectList()
                                    .map(items -> OrderMapper.toOrderResponseDto(order, orderItems, items));
                        })
                );
    }

    @Override
    @Transactional
    public Mono<Long> createOrder() {
        log.debug("Creating order from cart items");
        return cartItemRepository.findAll()
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty())
                        return Mono.error(new OrderConflictException("Order must contain at least one item."));

                    var itemIds = cartItems.stream().map(CartItem::getItemId).toList();

                    return itemRepository.findByIdIn(itemIds).collectList()
                            .flatMap(items -> {
                                var order = OrderMapper.toOrder(cartItems, items);
                                var amount = BigDecimal.valueOf(order.getTotalSum());
                                var holdRq = new HoldRq(amount);
                                return paymentAdapter.hold(holdRq)
                                        .onErrorResume(Mono::error)
                                        .flatMap(holdRs ->
                                                orderRepository.save(order)
                                                        .flatMap(savedOrder -> {
                                                            var orderId = savedOrder.getId();
                                                            log.debug("Order created with id={} and {} items", orderId, cartItems.size());
                                                            var orderItems = OrderMapper.toOrderItems(cartItems, items, orderId);

                                                            return orderItemRepository.saveAll(orderItems)
                                                                    .doOnComplete(() -> log.debug("Saved {} order items for order {}", orderItems.size(), orderId))
                                                                    .then(cartItemRepository.deleteAll()
                                                                            .doOnSuccess(v -> log.debug("Cart items cleared after order creation"))
                                                                    )
                                                                    .then(paymentAdapter.confirm(holdRs.paymentId())
                                                                            .onErrorResume(Mono::error))
                                                                    .thenReturn(orderId);
                                                        })
                                                        .onErrorResume(ex -> {
                                                            log.warn("DB failed. Start refund payment", ex);
                                                            return paymentAdapter.cancel(holdRs.paymentId())
                                                                    .onErrorResume(refundEx -> {
                                                                        log.error("DB and refund failed", refundEx);
                                                                        return Mono.empty();
                                                                    })
                                                                    .then(Mono.error(ex));
                                                        })
                                        );

                            });
                });
    }
}
