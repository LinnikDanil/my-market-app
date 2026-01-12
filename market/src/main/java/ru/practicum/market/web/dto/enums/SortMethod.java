package ru.practicum.market.web.dto.enums;

import lombok.Getter;

@Getter
public enum SortMethod {
    NO(null), // Без сортировки
    ALPHA("title"), // По названию
    PRICE("price"); // По цене

    private final String columnName;

    SortMethod(String columnName) {
        this.columnName = columnName;
    }
}
