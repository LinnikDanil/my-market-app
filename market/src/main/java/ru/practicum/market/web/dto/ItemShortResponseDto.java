package ru.practicum.market.web.dto;

/**
 * Укороченное DTO товара для административного списка.
 *
 * @param id    идентификатор товара
 * @param title название товара
 */
public record ItemShortResponseDto(long id, String title) {
}
