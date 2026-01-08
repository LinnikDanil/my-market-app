package ru.practicum.market.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.web.dto.OrderResponseDto;

public interface OrderService {
    Flux<OrderResponseDto> getOrders();

    Mono<OrderResponseDto> getOrder(long id);

    Mono<Long> createOrder();
}
