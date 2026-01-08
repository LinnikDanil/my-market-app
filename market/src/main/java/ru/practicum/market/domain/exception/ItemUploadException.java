package ru.practicum.market.domain.exception;

public class ItemUploadException extends RuntimeException {

    public ItemUploadException(String message) {
        super(message);
    }

    public ItemUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
