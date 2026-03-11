package ru.practicum.market.service.cache.dto;

import java.util.List;

/**
 * DTO кэша корзины.
 *
 * @param items список товаров корзины из кэша
 */
public record CartCacheDto(
    List<ItemCacheDto> items
) {
}
