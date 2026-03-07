package ru.practicum.market.integration.exception;

/**
 * Ошибка бизнес-конфликта платежа (например, недостаточно средств).
 */
public class PaymentBalanceException extends RuntimeException {
    /**
     * Создаёт исключение платежного конфликта.
     *
     * @param message текст ошибки
     */
    public PaymentBalanceException(String message) {
        super(message);
    }
}
