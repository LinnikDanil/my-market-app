package ru.practicum.market.web.bind.model;

import ru.practicum.market.web.dto.enums.SortMethod;

public record ItemsQuery(
        String search,
        SortMethod sort,
        int pageNumber,
        int pageSize
) {
}
