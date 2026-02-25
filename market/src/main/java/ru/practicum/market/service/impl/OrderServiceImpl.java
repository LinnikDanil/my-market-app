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
import ru.practicum.market.domain.model.Item;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final PaymentAdapter paymentAdapter;

    /**
     * Возвращает список всех заказов вместе с их позициями и товарами.
     */
    @Override
    @Transactional(readOnly = true)
    public Flux<OrderResponseDto> getOrders() {
        log.debug("Request to fetch all orders");
        return orderRepository.findAll()
                .collectList()
                .flatMapMany(this::loadOrderResponses);
    }

    /**
     * Возвращает один заказ по id вместе с его позициями и товарами.
     *
     * @param id идентификатор заказа
     */
    @Override
    @Transactional(readOnly = true)
    public Mono<OrderResponseDto> getOrder(long id) {
        log.debug("Request to fetch order with id={}", id);

        return findOrderById(id)
                .flatMap(order -> buildOrderResponse(order, id));
    }

    /**
     * Создает заказ из текущих позиций корзины:
     * резервирует платеж, сохраняет заказ и позиции, очищает корзину, подтверждает платеж.
     */
    @Override
    @Transactional
    public Mono<Long> createOrder() {
        log.debug("Creating order from cart items");
        return cartItemRepository.findAll()
                .collectList()
                .flatMap(this::createOrderFromCartItems);
    }

    /**
     * Загружает DTO заказов на основе списка заказов.
     * Если заказов нет, возвращает пустой поток.
     */
    private Flux<OrderResponseDto> loadOrderResponses(List<Order> orders) {
        if (orders.isEmpty()) {
            return Flux.empty();
        }

        var orderIds = orders.stream().map(Order::getId).toList();
        return orderItemRepository.findByOrderIdIn(orderIds)
                .collectList()
                .flatMapMany(orderItems -> loadOrderResponses(orders, orderItems));
    }

    /**
     * Достраивает DTO заказов: к заказам и позициям подтягивает карточки товаров.
     */
    private Flux<OrderResponseDto> loadOrderResponses(List<Order> orders, List<OrderItem> orderItems) {
        var itemIds = orderItems.stream().map(OrderItem::getItemId).toList();
        return itemRepository.findByIdIn(itemIds)
                .collectList()
                .flatMapMany(items -> {
                    var responses = OrderMapper.toOrderResponseDtos(orders, orderItems, items);
                    log.debug("Fetched {} orders", orders.size());
                    return Flux.fromIterable(responses);
                });
    }

    /**
     * Ищет заказ по id или бросает {@link OrderNotFoundException}.
     */
    private Mono<Order> findOrderById(long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(id, "Order with id = %d not found.".formatted(id))));
    }

    /**
     * Формирует DTO конкретного заказа: загружает позиции заказа и соответствующие товары.
     */
    private Mono<OrderResponseDto> buildOrderResponse(Order order, long orderId) {
        return orderItemRepository.findByOrderId(orderId)
                .collectList()
                .flatMap(orderItems -> {
                    var itemIds = orderItems.stream().map(OrderItem::getItemId).toList();
                    return itemRepository.findByIdIn(itemIds)
                            .collectList()
                            .map(items -> OrderMapper.toOrderResponseDto(order, orderItems, items));
                });
    }

    /**
     * Проверяет, что корзина не пуста, и продолжает создание заказа.
     */
    private Mono<Long> createOrderFromCartItems(List<CartItem> cartItems) {
        if (cartItems.isEmpty()) {
            return Mono.error(new OrderConflictException("Order must contain at least one item."));
        }

        var itemIds = cartItems.stream().map(CartItem::getItemId).toList();
        return itemRepository.findByIdIn(itemIds)
                .collectList()
                .flatMap(items -> placeOrder(cartItems, items));
    }

    /**
     * Создает заказ из корзины и запрашивает hold в платежном сервисе.
     */
    private Mono<Long> placeOrder(List<CartItem> cartItems, List<Item> items) {
        var order = OrderMapper.toOrder(cartItems, items);
        var holdRq = new HoldRq(BigDecimal.valueOf(order.getTotalSum()));

        return paymentAdapter.hold(holdRq)
                .onErrorResume(Mono::error)
                .flatMap(holdRs -> saveOrderAndConfirmPayment(order, cartItems, items, holdRs.paymentId()));
    }

    /**
     * Сохраняет заказ в БД и в случае ошибки пытается отменить резерв платежа.
     */
    private Mono<Long> saveOrderAndConfirmPayment(
            Order order,
            List<CartItem> cartItems,
            List<Item> items,
            UUID paymentId
    ) {
        return orderRepository.save(order)
                .flatMap(savedOrder -> persistOrderItemsAndConfirm(savedOrder, cartItems, items, paymentId))
                .onErrorResume(ex -> rollbackPaymentAfterDbFailure(ex, paymentId));
    }

    /**
     * Сохраняет позиции заказа, очищает корзину и подтверждает платеж.
     */
    private Mono<Long> persistOrderItemsAndConfirm(
            Order savedOrder,
            List<CartItem> cartItems,
            List<Item> items,
            UUID paymentId
    ) {
        var orderId = savedOrder.getId();
        log.debug("Order created with id={} and {} items", orderId, cartItems.size());
        var orderItems = OrderMapper.toOrderItems(cartItems, items, orderId);

        return orderItemRepository.saveAll(orderItems)
                .doOnComplete(() -> log.debug("Saved {} order items for order {}", orderItems.size(), orderId))
                .then(cartItemRepository.deleteAll()
                        .doOnSuccess(v -> log.debug("Cart items cleared after order creation"))
                )
                .then(paymentAdapter.confirm(paymentId)
                        .onErrorResume(Mono::error))
                .thenReturn(orderId);
    }

    /**
     * Компенсация при ошибке БД: отменяет резерв платежа и пробрасывает исходную ошибку.
     */
    private Mono<Long> rollbackPaymentAfterDbFailure(Throwable ex, UUID paymentId) {
        log.warn("DB failed. Start refund payment", ex);
        return paymentAdapter.cancel(paymentId)
                .onErrorResume(refundEx -> {
                    log.error("DB and refund failed", refundEx);
                    return Mono.empty();
                })
                .then(Mono.error(ex));
    }
}
