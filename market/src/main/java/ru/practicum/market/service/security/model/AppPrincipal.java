package ru.practicum.market.service.security.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Реализация {@link UserDetails}, используемая в security-контексте приложения.
 *
 * @param id          идентификатор пользователя
 * @param username    имя пользователя
 * @param password    хэш пароля
 * @param authorities набор ролей/прав пользователя
 */
public record AppPrincipal(
        Long id,
        String username,
        String password,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    /**
     * Возвращает набор granted authorities пользователя.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Возвращает пароль пользователя.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Возвращает username пользователя.
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Возвращает идентификатор пользователя.
     */
    public Long getId() {
        return id;
    }
}
