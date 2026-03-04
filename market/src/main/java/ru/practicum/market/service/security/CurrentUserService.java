package ru.practicum.market.service.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.security.model.AppPrincipal;

@Service
public class CurrentUserService {

    public Mono<Long> currentUserId(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getPrincipal)
                .cast(AppPrincipal.class)
                .map(AppPrincipal::id);
    }

    public Mono<Long> currentUserIdIfAuthenticated(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getPrincipal)
                .ofType(AppPrincipal.class)
                .map(AppPrincipal::id);
    }

}
