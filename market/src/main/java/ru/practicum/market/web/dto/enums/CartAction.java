package ru.practicum.market.web.dto.enums;

/**
 * Возможные действия с количеством товара в корзине.
 */
public enum CartAction {
    /** Уменьшить количество на 1. */
    MINUS,
    /** Увеличить количество на 1. */
    PLUS,
    /** Полностью удалить позицию из корзины. */
    DELETE
}
