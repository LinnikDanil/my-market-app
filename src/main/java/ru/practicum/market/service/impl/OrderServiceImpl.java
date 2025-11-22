package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.market.domain.exception.OrderNotFoundException;
import ru.practicum.market.repository.OrderRepository;
import ru.practicum.market.service.OrderService;
import ru.practicum.market.web.dto.OrderResponseDto;
import ru.practicum.market.web.mapper.OrderMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

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
}
