package ru.practicum.market.web.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.security.RepositoryUserDetailsService;
import ru.practicum.market.web.dto.AppUserRequestDto;
import ru.practicum.market.web.view.PageRenderHelper;

import java.net.URI;

@RequiredArgsConstructor
@Component
public class AuthHandler {

    private final PageRenderHelper pageRenderHelper;
    private final RepositoryUserDetailsService userService;

    /**
     * Отображает страницу аутентификации.
     */
    @PreAuthorize("permitAll()")
    public Mono<ServerResponse> login(ServerRequest request) {
        return pageRenderHelper.ok(request, "login");
    }

    /**
     * Отображает страницу регистрации.
     */
    @PreAuthorize("permitAll()")
    public Mono<ServerResponse> registerForm(ServerRequest request) {
        return pageRenderHelper.ok(request, "register");
    }

    /**
     * Отображает страницу ошибки доступа.
     */
    @PreAuthorize("permitAll()")
    public Mono<ServerResponse> accessDenied(ServerRequest request) {
        return pageRenderHelper.ok(request, "access-denied");
    }

    /**
     * Регистрация пользователя.
     */
    @PreAuthorize("permitAll()")
    public Mono<ServerResponse> register(ServerRequest request) {
        Mono<AppUserRequestDto> payload = request.formData()
                .map(form -> new AppUserRequestDto(
                        form.getFirst("username"),
                        form.getFirst("password")
                ));

        return userService.register(payload)
                .then(ServerResponse.seeOther(URI.create("/login")).build());
    }
}
