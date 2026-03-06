package ru.practicum.market.web.filter;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.*;
import ru.practicum.market.integration.exception.PaymentBalanceException;
import ru.practicum.market.integration.exception.PaymentIdNotFoundException;
import ru.practicum.market.integration.exception.PaymentServiceUnavailableException;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.service.security.CurrentUserService;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class RouteExceptionFilter {

    private final ItemService itemService;
    private final CurrentUserService userService;

    /**
     * Возвращает общий фильтр обработки исключений для functional routes.
     */
    public HandlerFilterFunction<ServerResponse, ServerResponse> errors() {
        return (request, next) ->
                next.handle(request)
                        .onErrorResume(ServerWebInputException.class, e -> badRequest(e, request))
                        .onErrorResume(WebExchangeBindException.class, e -> badRequest(e, request))
                        .onErrorResume(ValidationException.class, e -> badRequest(e, request))
                        .onErrorResume(MarketBadRequestException.class, e -> badRequest(e, request))

                        .onErrorResume(ItemImageBadRequest.class, e -> adminBadRequest(e, request))
                        .onErrorResume(ItemUploadException.class, e -> adminBadRequest(e, request))

                        .onErrorResume(UserAlreadyExistsException.class, e -> userAlreadyExists(e, request))
                        .onErrorResume(AccessDeniedException.class, e -> accessDenied(e, request))

                        .onErrorResume(NotFoundExceptionAbstract.class, e -> notFound(e, request))

                        .onErrorResume(PaymentIdNotFoundException.class, e -> paymentNofFound(e, request))
                        .onErrorResume(PaymentBalanceException.class, e -> insufficientFunds(e, request))
                        .onErrorResume(WebClientRequestException.class, e -> paymentServiceUnavailable(e, request))
                        .onErrorResume(PaymentServiceUnavailableException.class, e -> paymentServiceUnavailable(e, request))

                        .onErrorResume(OrderConflictException.class, e -> conflict(e, request))

                        .onErrorResume(Exception.class, e -> oops(e, request));
    }

    /**
     * Обрабатывает ошибку "payment id not found".
     */
    private Mono<ServerResponse> paymentNofFound(RuntimeException e, ServerRequest request) {
        logException(e);
        return render(request, HttpStatus.SERVICE_UNAVAILABLE, "errors/payment-not-found", Map.of());
    }

    /**
     * Обрабатывает нехватку средств и возвращает корзину с неактивной оплатой.
     */
    private Mono<ServerResponse> insufficientFunds(PaymentBalanceException e, ServerRequest request) {
        logException(e);

        return userService.currentUserId(request)
                .flatMap(itemService::getCartWithoutPayments)
                .flatMap(cart -> render(request, HttpStatus.CONFLICT, "cart", Map.of(
                                "items", cart.items(),
                                "total", cart.total(),
                                "isActive", false
                        )));
    }

    /**
     * Обрабатывает недоступность платежного сервиса.
     */
    private Mono<ServerResponse> paymentServiceUnavailable(RuntimeException e, ServerRequest request) {
        logException(e);

        return userService.currentUserId(request)
                .flatMap(itemService::getCartWithoutPayments)
                .flatMap(cart -> render(request, HttpStatus.SERVICE_UNAVAILABLE, "cart", Map.of(
                                "items", cart.items(),
                                "total", cart.total(),
                                "isActive", false,
                                "message", "Сервис платежей не доступен. Попробуйте позже."
                        )));
    }

    /**
     * Обрабатывает непредвиденные ошибки.
     */
    private Mono<ServerResponse> oops(Exception e, ServerRequest request) {
        log.error("Handled exception of type {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return render(request, HttpStatus.INTERNAL_SERVER_ERROR, "errors/oops", Map.of());
    }

    /**
     * Возвращает страницу ошибки 400.
     */
    private Mono<ServerResponse> badRequest(Exception e, ServerRequest request) {
        logException(e);
        return render(request, HttpStatus.BAD_REQUEST, "errors/bad-request", Map.of());
    }

    /**
     * Возвращает страницу ошибки 400 для admin-сценариев.
     */
    private Mono<ServerResponse> adminBadRequest(Exception e, ServerRequest request) {
        logException(e);
        return render(request, HttpStatus.BAD_REQUEST, "errors/admin-error", Map.of());
    }

    private Mono<ServerResponse> userAlreadyExists(Exception e, ServerRequest request) {
        logException(e);
        return render(request, HttpStatus.CONFLICT, "errors/user-exists", Map.of());
    }

    private Mono<ServerResponse> accessDenied(Exception e, ServerRequest request) {
        logException(e);
        return render(request, HttpStatus.FORBIDDEN, "errors/access-denied", Map.of());
    }

    /**
     * Возвращает страницу ошибки 404.
     */
    private Mono<ServerResponse> notFound(NotFoundExceptionAbstract e, ServerRequest request) {
        logException(e);
        return render(request, HttpStatus.NOT_FOUND, "errors/not-found", Map.of("id", String.valueOf(e.getId())));
    }

    /**
     * Возвращает страницу ошибки 409.
     */
    private Mono<ServerResponse> conflict(Exception e, ServerRequest request) {
        logException(e);
        return render(request, HttpStatus.CONFLICT, "errors/conflict", Map.of());
    }

    /**
     * Логирует обработанное исключение единообразно.
     */
    private void logException(Exception e) {
        log.warn("Handled exception of type {}: {}", e.getClass().getSimpleName(), e.getMessage());
    }

    /**
     * Рендерит страницу и гарантированно добавляет флаг authenticated в модель.
     */
    private Mono<ServerResponse> render(ServerRequest request,
                                        HttpStatus status,
                                        String view,
                                        Map<String, Object> model) {
        return request.principal()
                .map(Principal::getName)
                .map(name -> !"anonymousUser".equalsIgnoreCase(name))
                .defaultIfEmpty(false)
                .flatMap(authenticated -> {
                    Map<String, Object> enrichedModel = new HashMap<>(model);
                    enrichedModel.putIfAbsent("authenticated", authenticated);
                    return ServerResponse.status(status).render(view, enrichedModel);
                });
    }

}
