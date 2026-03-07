package ru.practicum.market.service.converter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Перечень заголовков колонок Excel-файла с товарами.
 */
@RequiredArgsConstructor
public enum ExcelItemColumn {
    TITLE("Title"),
    DESCRIPTION("Description"),
    PRICE("Price");

    @Getter
    private final String header;
}
