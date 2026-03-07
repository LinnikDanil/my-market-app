package ru.practicum.market.web.dto;

/**
 * DTO запроса регистрации пользователя.
 *
 * @param username логин пользователя
 * @param password пароль пользователя
 */
public record AppUserRequestDto(
        String username,
        String password
) {
}
