package ru.practicum.payments.exception;

import lombok.Getter;

import java.math.BigDecimal;

public class PaymentBalanceException extends RuntimeException {
    @Getter
    private final BigDecimal balance;

    public PaymentBalanceException(BigDecimal balance, String message) {
        super(message);
        this.balance = balance;
    }
}
