package ru.practicum.market.domain.exception;

import lombok.Getter;

@Getter
public class CartItemNotFoundException extends NotFoundExceptionAbstract {

    public CartItemNotFoundException(Long id, String message) {
        super(id, message);
    }
}
