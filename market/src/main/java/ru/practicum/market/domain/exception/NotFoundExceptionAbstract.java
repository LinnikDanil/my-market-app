package ru.practicum.market.domain.exception;

import lombok.Getter;

/**
 * Базовое исключение "сущность не найдена" c хранением идентификатора.
 */
@Getter
public class NotFoundExceptionAbstract extends RuntimeException {
    private final Long id;

    /**
     * Создаёт исключение not found.
     *
     * @param id      идентификатор сущности
     * @param message текст ошибки
     */
    public NotFoundExceptionAbstract(Long id, String message) {
        super(message);
        this.id = id;
    }
}
