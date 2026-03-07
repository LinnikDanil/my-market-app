package ru.practicum.market.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.UserAlreadyExistsException;
import ru.practicum.market.domain.exception.UserNotFoundException;
import ru.practicum.market.domain.model.AppUser;
import ru.practicum.market.repository.AppRoleRepository;
import ru.practicum.market.repository.AppUserRepository;
import ru.practicum.market.repository.AppUserRoleRepository;
import ru.practicum.market.service.cache.AppRoleCacheService;
import ru.practicum.market.service.security.model.AppPrincipal;
import ru.practicum.market.web.dto.AppUserRequestDto;

/**
 * Реализация {@link ReactiveUserDetailsService}, загружающая пользователей и роли из репозиториев.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryUserDetailsService implements ReactiveUserDetailsService {

    private static final String ROLE_USER = "ROLE_USER";

    private final AppUserRepository userRepository;
    private final AppRoleRepository roleRepository;
    private final AppUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppRoleCacheService appRoleCacheService;

    /**
     * Загружает пользователя по имени для Spring Security.
     *
     * @param username имя пользователя
     * @return объект {@link UserDetails} c ролями
     */
    @Override
    @PreAuthorize("permitAll()")
    public Mono<UserDetails> findByUsername(String username) {
        log.debug("Loading user details for username='{}'", username);
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User not found: username='{}'", username);
                    return Mono.error(new UserNotFoundException("User with name = %s not found".formatted(username)));
                }))
                .flatMap(appUser -> userRoleRepository.findRoleIdsByUserId(appUser.getId())
                        .collectList()
                        .flatMapMany(roleRepository::findByIdIn)
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collectList()
                        .map(roles ->
                                new AppPrincipal(appUser.getId(), appUser.getUsername(), appUser.getPassword(), roles)
                        )
                        .doOnSuccess(ignored -> log.debug("Loaded user details for username='{}'", username)));
    }

    /**
     * Регистрирует нового пользователя с ролью `ROLE_USER`.
     *
     * @param appUserRequestDto входной DTO с логином/паролем
     * @return сигнал завершения регистрации
     */
    @PreAuthorize("permitAll()")
    public Mono<Void> register(Mono<AppUserRequestDto> appUserRequestDto) {
        return appUserRequestDto.flatMap(dto -> {
            var encodedPassword = passwordEncoder.encode(dto.password());
            var username = dto.username();
            log.info("Registering new user username='{}'", username);

            return userRepository.save(new AppUser(username, encodedPassword))
                    .onErrorMap(DataIntegrityViolationException.class, ex ->
                            {
                                log.warn("User already exists: username='{}'", username);
                                return new UserAlreadyExistsException(
                                        "User with name = %s already exists.".formatted(username)
                                );
                            }
                    )
                    .flatMap(user -> appRoleCacheService.getRoleIdByName(ROLE_USER)
                            .flatMap(roleId -> userRoleRepository.save(user.getId(), roleId))
                            .doOnSuccess(ignored -> log.info("User registered username='{}', userId={}",
                                    username, user.getId())));
        });
    }
}
