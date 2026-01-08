package ru.practicum.market.domain.exception;

public class MarketBadRequestException extends RuntimeException {
    public MarketBadRequestException(String message) {
        super(message);
    }
}
