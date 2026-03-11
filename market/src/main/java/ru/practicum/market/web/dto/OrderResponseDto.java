package ru.practicum.market.web.dto;

import java.util.List;

/**
 * DTO заказа пользователя.
 *
 * @param id       идентификатор заказа
 * @param items    список товаров заказа
 * @param totalSum суммарная стоимость заказа
 */
public record OrderResponseDto(
        long id,
        List<ItemResponseDto> items,
        long totalSum
) {
    public OrderResponseDto {
        items = List.copyOf(items);
    }
}
