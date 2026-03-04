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
     * @param userId идентификатор пользователя
     * @return поток заказов
     */
    Flux<OrderResponseDto> getOrders(long userId);

    /**
     * Возвращает один заказ по идентификатору.
     *
     * @param userId
     * @param orderId идентификатор заказа
     * @return DTO заказа
     */
    Mono<OrderResponseDto> getOrder(long userId, long orderId);

    /**
     * Создает заказ из текущей корзины.
     *
     * @param userId идентификатор пользователя
     * @return идентификатор созданного заказа
     */
    Mono<Long> createOrder(long userId);
}
