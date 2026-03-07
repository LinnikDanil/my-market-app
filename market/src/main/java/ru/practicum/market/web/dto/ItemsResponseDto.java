package ru.practicum.market.web.dto;

import ru.practicum.market.web.dto.enums.SortMethod;

import java.util.List;

/**
 * DTO страницы каталога товаров.
 *
 * @param items  список строк товаров для отображения в UI
 * @param search строка поиска
 * @param sort   выбранный режим сортировки
 * @param paging параметры пагинации
 */
public record ItemsResponseDto(
        List<List<ItemResponseDto>> items,
        String search,
        SortMethod sort,
        Paging paging
) {
    public ItemsResponseDto {
        items = List.copyOf(items);
    }
}
