package ru.practicum.market.service;

import ru.practicum.market.web.dto.OrderResponseDto;

import java.util.List;

public interface OrderService {
    List<OrderResponseDto> getOrders();

    OrderResponseDto getOrder(long id);
}
