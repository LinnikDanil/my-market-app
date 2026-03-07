package ru.practicum.market.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO запроса регистрации пользователя.
 *
 * @param username логин пользователя
 * @param password пароль пользователя
 */
public record AppUserRequestDto(
        @NotBlank(message = "username must not be blank")
        @Size(min = 3, max = 100, message = "username size must be between 3 and 100")
        String username,
        @NotBlank(message = "password must not be blank")
        @Size(min = 5, max = 100, message = "password size must be between 5 and 100")
        String password
) {
}
