package ru.practicum.market.web.dto.enums;

import lombok.Getter;

/**
 * Поддерживаемые режимы сортировки каталога.
 */
@Getter
public enum SortMethod {
    /** Без сортировки. */
    NO(null),
    /** Сортировка по названию. */
    ALPHA("title"),
    /** Сортировка по цене. */
    PRICE("price");

    private final String columnName;

    SortMethod(String columnName) {
        this.columnName = columnName;
    }
}
