package ru.practicum.market.web.view;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class PageRenderHelper {
    private final WebSessionServerCsrfTokenRepository csrfTokenRepository;

    public PageRenderHelper(WebSessionServerCsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    public Mono<ServerResponse> ok(ServerRequest request, String viewName) {
        return render(request, HttpStatus.OK, viewName, Map.of());
    }

    public Mono<ServerResponse> ok(ServerRequest request, String viewName, Map<String, Object> model) {
        return render(request, HttpStatus.OK, viewName, model);
    }

    private Mono<ServerResponse> render(ServerRequest request,
                                        HttpStatusCode status,
                                        String viewName,
                                        Map<String, Object> model) {
        return enrichModelWithCsrf(request, model)
                .flatMap(enrichedModel -> ServerResponse.status(status)
                        .contentType(MediaType.TEXT_HTML)
                        .render(viewName, enrichedModel));
    }

    private Mono<Map<String, Object>> enrichModelWithCsrf(ServerRequest request, Map<String, Object> model) {
        Mono<CsrfToken> csrfTokenMono = request.exchange().getAttribute(CsrfToken.class.getName());
        Mono<CsrfToken> effectiveCsrfTokenMono = csrfTokenMono == null
                ? generateAndSaveToken(request)
                : csrfTokenMono.switchIfEmpty(generateAndSaveToken(request));

        Mono<Boolean> authenticatedMono = request.principal()
                .map(Principal::getName)
                .map(name -> !"anonymousUser".equalsIgnoreCase(name))
                .defaultIfEmpty(false)
                .onErrorReturn(false);

        return authenticatedMono.flatMap(authenticated -> effectiveCsrfTokenMono
                .map(csrfToken -> {
                    Map<String, Object> enrichedModel = new HashMap<>(model);
                    enrichedModel.put("authenticated", authenticated);
                    enrichedModel.putIfAbsent("_csrf", csrfToken);
                    return Collections.unmodifiableMap(enrichedModel);
                })
                .defaultIfEmpty(mapWithAuthenticated(model, authenticated)));
    }

    private Mono<CsrfToken> generateAndSaveToken(ServerRequest request) {
        return csrfTokenRepository.generateToken(request.exchange())
                .flatMap(token -> csrfTokenRepository.saveToken(request.exchange(), token).thenReturn(token));
    }

    private Map<String, Object> mapWithAuthenticated(Map<String, Object> model, boolean authenticated) {
        Map<String, Object> enrichedModel = new HashMap<>(model);
        enrichedModel.put("authenticated", authenticated);
        return Collections.unmodifiableMap(enrichedModel);
    }
}
