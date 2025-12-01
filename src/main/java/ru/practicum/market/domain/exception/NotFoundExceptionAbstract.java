package ru.practicum.market.domain.exception;

import lombok.Getter;

@Getter
public class NotFoundExceptionAbstract extends RuntimeException {
    private final Long id;

    public NotFoundExceptionAbstract(Long id, String message) {
        super(message);
        this.id = id;
    }
}
