package ru.practicum.market.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.security.model.AppPrincipal;

/**
 * Сервис извлечения идентификатора текущего пользователя из security-контекста запроса.
 */
@Service
@Slf4j
public class CurrentUserService {

    /**
     * Возвращает идентификатор текущего аутентифицированного пользователя.
     *
     * @param request входящий HTTP-запрос
     * @return идентификатор пользователя
     */
    public Mono<Long> currentUserId(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getPrincipal)
                .cast(AppPrincipal.class)
                .map(AppPrincipal::id)
                .doOnNext(userId -> log.trace("Resolved authenticated userId={}", userId));
    }

    /**
     * Возвращает идентификатор пользователя, если в контексте есть валидный principal.
     *
     * @param request входящий HTTP-запрос
     * @return идентификатор пользователя или пустой сигнал
     */
    public Mono<Long> currentUserIdIfAuthenticated(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getPrincipal)
                .ofType(AppPrincipal.class)
                .map(AppPrincipal::id)
                .doOnNext(userId -> log.trace("Resolved optional authenticated userId={}", userId));
    }

}
