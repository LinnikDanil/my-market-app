package ru.practicum.market.integration.exception;

public class PaymentBalanceException extends RuntimeException {
    public PaymentBalanceException(String message) {
        super(message);
    }
}
