package ru.practicum.market.domain.exception;

/**
 * Ошибка валидации входного запроса маркет-модуля.
 */
public class MarketBadRequestException extends RuntimeException {
    /**
     * Создаёт исключение bad request.
     *
     * @param message текст ошибки
     */
    public MarketBadRequestException(String message) {
        super(message);
    }
}
