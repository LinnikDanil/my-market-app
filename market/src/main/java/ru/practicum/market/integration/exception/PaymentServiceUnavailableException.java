package ru.practicum.market.integration.exception;

/**
 * Ошибка недоступности внешнего платежного сервиса.
 */
public class PaymentServiceUnavailableException extends RuntimeException {
    /**
     * Создаёт исключение недоступности сервиса.
     *
     * @param message текст ошибки
     */
    public PaymentServiceUnavailableException(String message) {
        super(message);
    }
}
