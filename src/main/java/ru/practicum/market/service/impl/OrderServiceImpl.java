package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.market.domain.exception.OrderConflictException;
import ru.practicum.market.domain.exception.OrderNotFoundException;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.repository.OrderRepository;
import ru.practicum.market.service.OrderService;
import ru.practicum.market.web.dto.OrderResponseDto;
import ru.practicum.market.web.mapper.OrderMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrders() {
        var orders = orderRepository.findAll();

        return OrderMapper.toOrderResponseDtos(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(long id) {
        return orderRepository.findById(id)
                .map(OrderMapper::toOrderResponseDto)
                .orElseThrow(() -> new OrderNotFoundException("Order with id = %d not found.".formatted(id)));
    }

    @Override
    @Transactional
    public long createOrder() {
        var items = itemRepository.findByCountGreaterThan(0);

        if (items.isEmpty()) {
            throw new OrderConflictException("Order must contain at least one item.");
        }

        var newOrder = OrderMapper.toOrder(items);
        var savedOrder = orderRepository.save(newOrder);

        return savedOrder.getId();
    }
}
