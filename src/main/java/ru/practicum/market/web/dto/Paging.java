package ru.practicum.market.web.dto;

public record Paging(
        int pageSize, // Число товаров на странице
        int pageNumber, // Номер страницы
        boolean hasPrevious, // true, если не первая страница
        boolean hasNext // true, если не последняя страница
) {
}
