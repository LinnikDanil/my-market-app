package ru.practicum.market.web.bind.model;

import ru.practicum.market.web.dto.enums.SortMethod;

/**
 * DTO параметров запроса списка товаров.
 *
 * @param search     строка поиска
 * @param sort       способ сортировки
 * @param pageNumber номер страницы (с 1)
 * @param pageSize   размер страницы
 */
public record ItemsQuery(
        String search,
        SortMethod sort,
        int pageNumber,
        int pageSize
) {
}
