package ru.practicum.market.web.dto;

/**
 * DTO представления товара для UI.
 *
 * @param id          идентификатор товара
 * @param title       название товара
 * @param description описание товара
 * @param imgPath     URI/путь изображения
 * @param price       цена товара
 * @param count       количество товара в корзине пользователя
 */
public record ItemResponseDto(
        long id,
        String title,
        String description,
        String imgPath,
        long price,
        int count
) {
}
