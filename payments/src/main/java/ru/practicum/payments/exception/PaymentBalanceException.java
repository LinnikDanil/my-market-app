package ru.practicum.payments.exception;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Ошибка недостаточного баланса при попытке hold-операции.
 */
public class PaymentBalanceException extends RuntimeException {
    @Getter
    private final BigDecimal balance;

    /**
     * Создаёт исключение нехватки средств.
     *
     * @param balance текущий баланс пользователя
     * @param message текст ошибки
     */
    public PaymentBalanceException(BigDecimal balance, String message) {
        super(message);
        this.balance = balance;
    }
}
