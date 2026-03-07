package ru.practicum.market.web.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.security.RepositoryUserDetailsService;
import ru.practicum.market.web.dto.AppUserRequestDto;
import ru.practicum.market.web.view.PageRenderHelper;

import java.net.URI;

/**
 * Обработчик HTTP-сценариев аутентификации и регистрации.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class AuthHandler {

    private final PageRenderHelper pageRenderHelper;
    private final RepositoryUserDetailsService userService;

    /**
     * Отображает страницу аутентификации.
     *
     * @param request входящий HTTP-запрос
     * @return серверный ответ со страницей входа
     */
    @PreAuthorize("permitAll()")
    public Mono<ServerResponse> login(ServerRequest request) {
        log.debug("Rendering login page");
        return pageRenderHelper.ok(request, "login");
    }

    /**
     * Отображает страницу регистрации.
     *
     * @param request входящий HTTP-запрос
     * @return серверный ответ со страницей регистрации
     */
    @PreAuthorize("permitAll()")
    public Mono<ServerResponse> registerForm(ServerRequest request) {
        log.debug("Rendering register page");
        return pageRenderHelper.ok(request, "register");
    }

    /**
     * Отображает страницу ошибки доступа.
     *
     * @param request входящий HTTP-запрос
     * @return серверный ответ со страницей ошибки доступа
     */
    @PreAuthorize("permitAll()")
    public Mono<ServerResponse> accessDenied(ServerRequest request) {
        log.debug("Rendering access denied page");
        return pageRenderHelper.ok(request, "errors/access-denied");
    }

    /**
     * Регистрация пользователя.
     *
     * @param request входящий HTTP-запрос
     * @return редирект на страницу логина после успешной регистрации
     */
    @PreAuthorize("permitAll()")
    public Mono<ServerResponse> register(ServerRequest request) {
        Mono<AppUserRequestDto> payload = request.formData()
                .map(form -> new AppUserRequestDto(
                        form.getFirst("username"),
                        form.getFirst("password")
                ))
                .doOnNext(dto -> log.info("User registration requested for username='{}'", dto.username()));

        return userService.register(payload)
                .then(ServerResponse.seeOther(URI.create("/login")).build());
    }
}
