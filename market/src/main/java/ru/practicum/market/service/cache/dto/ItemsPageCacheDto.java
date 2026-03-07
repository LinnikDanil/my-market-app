package ru.practicum.market.service.cache.dto;

import java.util.List;

/**
 * DTO кэшированной страницы товаров.
 *
 * @param items      элементы текущей страницы
 * @param itemsCount общее количество товаров по запросу
 */
public record ItemsPageCacheDto(
        List<ItemCacheDto> items,
        long itemsCount
) {
}
