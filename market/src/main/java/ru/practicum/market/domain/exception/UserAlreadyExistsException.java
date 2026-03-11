package ru.practicum.market.domain.exception;

/**
 * Ошибка попытки регистрации уже существующего пользователя.
 */
public class UserAlreadyExistsException extends RuntimeException {
    /**
     * Создаёт исключение дублирования пользователя.
     *
     * @param message текст ошибки
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
