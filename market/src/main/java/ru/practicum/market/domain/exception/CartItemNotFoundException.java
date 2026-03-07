package ru.practicum.market.domain.exception;

import lombok.Getter;

/**
 * Ошибка отсутствия позиции товара в корзине.
 */
@Getter
public class CartItemNotFoundException extends NotFoundExceptionAbstract {

    /**
     * Создаёт исключение отсутствия элемента корзины.
     *
     * @param id      идентификатор элемента
     * @param message текст ошибки
     */
    public CartItemNotFoundException(Long id, String message) {
        super(id, message);
    }
}
