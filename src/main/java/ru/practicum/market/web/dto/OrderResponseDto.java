package ru.practicum.market.web.dto;

import java.util.List;

public record OrderResponseDto(
        long id,
        List<ItemResponseDto> items, // Товары
        long totalSum // Суммарная стоимость заказа
) {
    public OrderResponseDto {
        items = List.copyOf(items);
    }
}
