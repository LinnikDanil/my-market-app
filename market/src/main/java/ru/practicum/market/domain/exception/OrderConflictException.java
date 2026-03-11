package ru.practicum.market.domain.exception;

/**
 * Ошибка бизнес-конфликта при работе с заказом.
 */
public class OrderConflictException extends RuntimeException {
    /**
     * Создаёт исключение конфликта заказа.
     *
     * @param message текст ошибки
     */
    public OrderConflictException(String message) {
        super(message);
    }
}
