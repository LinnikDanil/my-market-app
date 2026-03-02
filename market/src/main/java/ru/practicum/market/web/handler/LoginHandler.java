package ru.practicum.market.web.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ru.practicum.market.web.view.PageRenderHelper;

@RequiredArgsConstructor
@Component
public class LoginHandler {

    private final PageRenderHelper pageRenderHelper;

    /**
     * Отображает страницу аутентификации.
     */
    @PreAuthorize("permitAll()")
    public Mono<ServerResponse> login(ServerRequest request) {
        return pageRenderHelper.ok(request, "login");
    }
}
