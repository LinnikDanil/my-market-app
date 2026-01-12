package ru.practicum.market.service.cache.dto;

import java.util.List;

public record ItemsPageCacheDto(
        List<ItemCacheDto> items,
        long itemsCount
) {
}
