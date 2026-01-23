package ru.practicum.market.service.converter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ExcelItemColumn {
    TITLE("Title"),
    DESCRIPTION("Description"),
    PRICE("Price");

    @Getter
    private final String header;
}
