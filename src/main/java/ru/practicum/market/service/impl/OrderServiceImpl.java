package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.market.domain.exception.OrderConflictException;
import ru.practicum.market.domain.exception.OrderNotFoundException;
import ru.practicum.market.repository.CartItemRepository;
import ru.practicum.market.repository.OrderItemRepository;
import ru.practicum.market.repository.OrderRepository;
import ru.practicum.market.service.OrderService;
import ru.practicum.market.web.dto.OrderResponseDto;
import ru.practicum.market.web.mapper.OrderMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrders() {
        log.info("Request to fetch all orders");
        var orders = orderRepository.findAllFetch();

        log.debug("Fetched {} orders", orders.size());
        return OrderMapper.toOrderResponseDtos(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(long id) {
        log.info("Request to fetch order with id={}", id);
        var order = orderRepository.findByIdFetch(id)
                .orElseThrow(() -> new OrderNotFoundException(id, "Order with id = %d not found.".formatted(id)));

        return OrderMapper.toOrderResponseDto(order);
    }

    @Override
    @Transactional
    public long createOrder() {
        log.info("Creating order from cart items");
        var cartItems = cartItemRepository.findAllFetch();

        if (cartItems.isEmpty()) {
            throw new OrderConflictException("Order must contain at least one item.");
        }

        var newOrder = OrderMapper.toOrder(cartItems);
        var savedOrder = orderRepository.save(newOrder);
        log.debug("Order created with id={} and {} items", savedOrder.getId(), cartItems.size());

        var orderItems = OrderMapper.toOrderItems(cartItems, savedOrder);
        orderItemRepository.saveAll(orderItems);

        log.info("Saved {} order items for order {}", orderItems.size(), savedOrder.getId());

        cartItemRepository.deleteAll();
        log.debug("Cart items cleared after order creation");

        return savedOrder.getId();
    }
}
