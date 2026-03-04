package ru.practicum.market.service.security;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class RepositoryUserDetailsService implements ReactiveUserDetailsService {

    private static final String ROLE_USER = "ROLE_USER";

    private final AppUserRepository userRepository;
    private final AppRoleRepository roleRepository;
    private final AppUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppRoleCacheService appRoleCacheService;

    @Override
    @PreAuthorize("permitAll()")
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(
                        Mono.error(new UserNotFoundException("User with name = %s not found".formatted(username)))
                )
                .flatMap(appUser -> userRoleRepository.findRoleIdsByUserId(appUser.getId())
                        .collectList()
                        .flatMapMany(roleRepository::findByIdIn)
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collectList()
                        .map(roles ->
                                new AppPrincipal(appUser.getId(), appUser.getUsername(), appUser.getPassword(), roles)
                        ));
    }

    @PreAuthorize("permitAll()")
    public Mono<Void> register(Mono<AppUserRequestDto> appUserRequestDto) {
        return appUserRequestDto.flatMap(dto -> {
            var encodedPassword = passwordEncoder.encode(dto.password());
            var username = dto.username();

            return userRepository.save(new AppUser(username, encodedPassword))
                    .onErrorMap(DataIntegrityViolationException.class, ex ->
                            new UserAlreadyExistsException("User with name = %s already exists.".formatted(username))
                    )
                    .flatMap(user -> appRoleCacheService.getRoleIdByName(ROLE_USER)
                            .flatMap(roleId -> userRoleRepository.save(user.getId(), roleId))
                    );
        });
    }
}
