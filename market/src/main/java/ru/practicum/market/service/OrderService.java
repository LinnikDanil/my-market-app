package ru.practicum.market.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.web.dto.OrderResponseDto;

/**
 * Сервис пользовательских сценариев работы с заказами.
 */
public interface OrderService {
    /**
     * Возвращает список всех заказов.
     *
     * @return поток заказов
     */
    Flux<OrderResponseDto> getOrders();

    /**
     * Возвращает один заказ по идентификатору.
     *
     * @param id идентификатор заказа
     * @return DTO заказа
     */
    Mono<OrderResponseDto> getOrder(long id);

    /**
     * Создает заказ из текущей корзины.
     *
     * @return идентификатор созданного заказа
     */
    Mono<Long> createOrder();
}
