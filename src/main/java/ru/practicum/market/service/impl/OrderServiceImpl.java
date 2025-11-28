package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
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
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrders() {
        var orders = orderRepository.findAllFetch();

        return OrderMapper.toOrderResponseDtos(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(long id) {
        var order = orderRepository.findByIdFetch(id)
                .orElseThrow(() -> new OrderNotFoundException("Order with id = %d not found.".formatted(id)));

        return OrderMapper.toOrderResponseDto(order);
    }

    @Override
    @Transactional
    public long createOrder() {
        var cartItems = cartItemRepository.findAllFetch();

        if (cartItems.isEmpty()) {
            throw new OrderConflictException("Order must contain at least one item.");
        }

        var newOrder = OrderMapper.toOrder(cartItems);
        var savedOrder = orderRepository.save(newOrder);

        var orderItems = OrderMapper.toOrderItems(cartItems, savedOrder);
        orderItemRepository.saveAll(orderItems);

        cartItemRepository.deleteAll();

        return savedOrder.getId();
    }
}
