package ru.practicum.payments.exception;

/**
 * Ошибка отсутствия hold-операции по указанному идентификатору.
 */
public class PaymentNotFoundException extends RuntimeException {
    /**
     * Создаёт исключение отсутствующего hold-платежа.
     *
     * @param message текст ошибки
     */
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
