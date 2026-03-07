package ru.practicum.market.service.cache.dto;

/**
 * DTO товара, используемый в кэше каталога.
 *
 * @param id          идентификатор товара
 * @param title       название товара
 * @param description описание товара
 * @param imgPath     URI/путь изображения
 * @param price       цена товара
 */
public record ItemCacheDto(
        long id,
        String title,
        String description,
        String imgPath,
        long price
) {
}
