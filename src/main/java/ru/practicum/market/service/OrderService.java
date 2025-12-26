package ru.practicum.market.service;

import reactor.core.publisher.Flux;
import ru.practicum.market.web.dto.OrderResponseDto;

public interface OrderService {
    Flux<OrderResponseDto> getOrders();

    OrderResponseDto getOrder(long id);

    long createOrder();
}
