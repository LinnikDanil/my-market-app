package ru.practicum.market.web.view;

import lombok.extern.slf4j.Slf4j;
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

/**
 * Вспомогательный компонент для рендера HTML-страниц с общим обогащением модели (CSRF, auth).
 */
@Component
@Slf4j
public class PageRenderHelper {
    private final WebSessionServerCsrfTokenRepository csrfTokenRepository;

    /**
     * Создаёт helper рендера страниц.
     *
     * @param csrfTokenRepository репозиторий CSRF-токенов
     */
    public PageRenderHelper(WebSessionServerCsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    /**
     * Возвращает HTML-ответ со статусом 200 и пустой моделью.
     *
     * @param request  входящий HTTP-запрос
     * @param viewName имя шаблона
     * @return серверный ответ
     */
    public Mono<ServerResponse> ok(ServerRequest request, String viewName) {
        return render(request, HttpStatus.OK, viewName, Map.of());
    }

    /**
     * Возвращает HTML-ответ со статусом 200 и переданной моделью.
     *
     * @param request  входящий HTTP-запрос
     * @param viewName имя шаблона
     * @param model    модель для рендера
     * @return серверный ответ
     */
    public Mono<ServerResponse> ok(ServerRequest request, String viewName, Map<String, Object> model) {
        return render(request, HttpStatus.OK, viewName, model);
    }

    /**
     * Рендерит страницу с заданным HTTP-статусом и обогащённой моделью.
     */
    private Mono<ServerResponse> render(ServerRequest request,
                                        HttpStatusCode status,
                                        String viewName,
                                        Map<String, Object> model) {
        log.debug("Rendering view='{}' with status={}", viewName, status.value());
        return enrichModelWithCsrf(request, model)
                .flatMap(enrichedModel -> ServerResponse.status(status)
                        .contentType(MediaType.TEXT_HTML)
                        .render(viewName, enrichedModel));
    }

    /**
     * Обогащает модель признаками аутентификации и CSRF-токеном.
     */
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

    /**
     * Генерирует и сохраняет CSRF-токен в текущей сессии.
     */
    private Mono<CsrfToken> generateAndSaveToken(ServerRequest request) {
        return csrfTokenRepository.generateToken(request.exchange())
                .flatMap(token -> csrfTokenRepository.saveToken(request.exchange(), token).thenReturn(token));
    }

    /**
     * Возвращает неизменяемую модель с признаком аутентификации.
     */
    private Map<String, Object> mapWithAuthenticated(Map<String, Object> model, boolean authenticated) {
        Map<String, Object> enrichedModel = new HashMap<>(model);
        enrichedModel.put("authenticated", authenticated);
        return Collections.unmodifiableMap(enrichedModel);
    }
}
