package ru.practicum.market.web.dto;

/**
 * DTO пагинации списка товаров.
 *
 * @param pageSize    размер страницы
 * @param pageNumber  номер текущей страницы (с 1)
 * @param hasPrevious признак наличия предыдущей страницы
 * @param hasNext     признак наличия следующей страницы
 */
public record Paging(
        int pageSize,
        int pageNumber,
        boolean hasPrevious,
        boolean hasNext
) {
}
