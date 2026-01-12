package ru.practicum.market.domain.exception;

public class ItemImageBadRequest extends RuntimeException {
    public ItemImageBadRequest(String message) {
        super(message);
    }

    public ItemImageBadRequest(String message, Throwable cause) {
        super(message, cause);
    }
}
