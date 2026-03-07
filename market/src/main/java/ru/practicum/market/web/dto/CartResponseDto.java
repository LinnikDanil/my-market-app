package ru.practicum.market.web.dto;

import java.util.List;

/**
 * DTO корзины пользователя.
 *
 * @param items          список товаров в корзине
 * @param total          суммарная стоимость корзины
 * @param isActiveButton признак доступности кнопки оформления заказа
 */
public record CartResponseDto(
        List<ItemResponseDto> items,
        long total,
        boolean isActiveButton
) {
    public CartResponseDto {
        items = List.copyOf(items);
    }
}
