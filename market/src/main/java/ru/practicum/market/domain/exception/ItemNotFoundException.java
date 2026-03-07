package ru.practicum.market.domain.exception;

/**
 * Ошибка отсутствия товара.
 */
public class ItemNotFoundException extends NotFoundExceptionAbstract {
    /**
     * Создаёт исключение отсутствия товара.
     *
     * @param id      идентификатор товара
     * @param message текст ошибки
     */
    public ItemNotFoundException(Long id, String message) {
        super(id, message);
    }
}
