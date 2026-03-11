package ru.practicum.market.domain.exception;

/**
 * Ошибка отсутствия заказа.
 */
public class OrderNotFoundException extends NotFoundExceptionAbstract {

    /**
     * Создаёт исключение отсутствия заказа.
     *
     * @param id      идентификатор заказа
     * @param message текст ошибки
     */
    public OrderNotFoundException(Long id, String message) {
        super(id, message);
    }
}
