package ru.practicum.market.web.dto;

import java.util.List;

public record CartResponseDto(
        List<ItemResponseDto> items, // Список товаров в корзине
        long total, // Суммарная цена товаров в корзине
        boolean isActiveButton // Активна ли кнопка заказа
) {
    public CartResponseDto {
        items = List.copyOf(items);
    }
}
