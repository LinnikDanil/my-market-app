package ru.practicum.market.service.cache.dto;

import java.util.List;

public record CartCacheDto(
    List<ItemCacheDto> items
) {
}
