package ru.practicum.market.service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.UserNotFoundException;
import ru.practicum.market.repository.AppRoleRepository;
import ru.practicum.market.repository.AppUserRepository;
import ru.practicum.market.repository.AppUserRoleRepository;

@Service
@RequiredArgsConstructor
public class RepositoryUserDetailsService implements ReactiveUserDetailsService {

    private final AppUserRepository userRepository;
    private final AppRoleRepository roleRepository;
    private final AppUserRoleRepository userRoleRepository;

    @Override
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
                                User.withUsername(appUser.getUsername())
                                        .password(appUser.getPassword())
                                        .authorities(roles)
                                        .build()
                        ));
    }
}
