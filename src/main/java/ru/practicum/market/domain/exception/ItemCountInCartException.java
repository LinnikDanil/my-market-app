package ru.practicum.market.domain.exception;

public class ItemCountInCartException extends RuntimeException {
    public ItemCountInCartException(String message) {
        super(message);
    }
}
