package ru.practicum.market.domain.exception;

/**
 * Ошибка загрузки товаров/файлов в административных сценариях.
 */
public class ItemUploadException extends RuntimeException {

    /**
     * Создаёт исключение с сообщением ошибки.
     *
     * @param message текст ошибки
     */
    public ItemUploadException(String message) {
        super(message);
    }

    /**
     * Создаёт исключение с причиной.
     *
     * @param message текст ошибки
     * @param cause   исходная причина
     */
    public ItemUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
