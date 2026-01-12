package ru.practicum.market.service.cache.dto;

public record ItemCacheDto(
        long id,
        String title,
        String description,
        String imgPath,
        long price
) {
}
