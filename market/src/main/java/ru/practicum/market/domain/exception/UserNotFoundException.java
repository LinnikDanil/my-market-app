package ru.practicum.market.domain.exception;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Ошибка отсутствия пользователя по имени.
 */
public class UserNotFoundException extends UsernameNotFoundException {
    /**
     * Создаёт исключение отсутствия пользователя.
     *
     * @param message текст ошибки
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
