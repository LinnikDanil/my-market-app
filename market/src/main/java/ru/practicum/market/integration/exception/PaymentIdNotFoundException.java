package ru.practicum.market.integration.exception;

/**
 * Ошибка отсутствия hold/payment идентификатора в платежном сервисе.
 */
public class PaymentIdNotFoundException extends RuntimeException {
    /**
     * Создаёт исключение not found для платежа.
     *
     * @param message текст ошибки
     */
    public PaymentIdNotFoundException(String message) {
        super(message);
    }
}
