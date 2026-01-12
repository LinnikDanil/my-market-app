package ru.practicum.market.web.dto;

import ru.practicum.market.web.dto.enums.SortMethod;

import java.util.List;

public record ItemsResponseDto(
        List<List<ItemResponseDto>> items, // Список из списка по три товара
        String search, // Строка поиска
        SortMethod sort, // Способ сортировки
        Paging paging // Страница
) {
    public ItemsResponseDto {
        items = List.copyOf(items);
    }
}
