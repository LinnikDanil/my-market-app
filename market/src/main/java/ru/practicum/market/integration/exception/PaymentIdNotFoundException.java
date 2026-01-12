package ru.practicum.market.integration.exception;

public class PaymentIdNotFoundException extends RuntimeException {
    public PaymentIdNotFoundException(String message) {
        super(message);
    }
}
