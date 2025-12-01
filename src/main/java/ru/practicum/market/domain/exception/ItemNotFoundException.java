package ru.practicum.market.domain.exception;

public class ItemNotFoundException extends NotFoundExceptionAbstract {
    public ItemNotFoundException(Long id, String message) {
        super(id, message);
    }
}
