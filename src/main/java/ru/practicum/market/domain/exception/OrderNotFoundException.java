package ru.practicum.market.domain.exception;

public class OrderNotFoundException extends NotFoundExceptionAbstract {

    public OrderNotFoundException(Long id, String message) {
        super(id, message);
    }
}
