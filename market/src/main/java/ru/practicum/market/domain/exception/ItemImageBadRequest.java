package ru.practicum.market.domain.exception;

/**
 * Ошибка некорректного запроса, связанного с изображением товара.
 */
public class ItemImageBadRequest extends RuntimeException {
    /**
     * Создаёт исключение с сообщением ошибки.
     *
     * @param message текст ошибки
     */
    public ItemImageBadRequest(String message) {
        super(message);
    }

    /**
     * Создаёт исключение с причиной.
     *
     * @param message текст ошибки
     * @param cause   исходная причина
     */
    public ItemImageBadRequest(String message, Throwable cause) {
        super(message, cause);
    }
}
