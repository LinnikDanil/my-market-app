package ru.practicum.market.web.filter;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.ItemImageBadRequest;
import ru.practicum.market.domain.exception.ItemUploadException;
import ru.practicum.market.domain.exception.MarketBadRequestException;
import ru.practicum.market.domain.exception.NotFoundExceptionAbstract;
import ru.practicum.market.domain.exception.OrderConflictException;
import ru.practicum.market.integration.exception.PaymentBalanceException;
import ru.practicum.market.integration.exception.PaymentIdNotFoundException;
import ru.practicum.market.integration.exception.PaymentServiceUnavailableException;
import ru.practicum.market.service.ItemService;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class RouteExceptionFilter {

    private final ItemService itemService;

    public HandlerFilterFunction<ServerResponse, ServerResponse> errors() {
        return (request, next) ->
                next.handle(request)
                        .onErrorResume(ServerWebInputException.class, e -> badRequest(e, request))
                        .onErrorResume(WebExchangeBindException.class, e -> badRequest(e, request))
                        .onErrorResume(ValidationException.class, e -> badRequest(e, request))
                        .onErrorResume(MarketBadRequestException.class, e -> badRequest(e, request))

                        .onErrorResume(ItemImageBadRequest.class, e -> adminBadRequest(e, request))
                        .onErrorResume(ItemUploadException.class, e -> adminBadRequest(e, request))

                        .onErrorResume(NotFoundExceptionAbstract.class, e -> notFound(e, request))

                        .onErrorResume(PaymentIdNotFoundException.class, e -> paymentNofFound(e, request))
                        .onErrorResume(PaymentBalanceException.class, e -> insufficientFunds(e, request))
                        .onErrorResume(WebClientRequestException.class, e -> paymentServiceUnavailable(e, request))
                        .onErrorResume(PaymentServiceUnavailableException.class, e -> paymentServiceUnavailable(e, request))

                        .onErrorResume(OrderConflictException.class, e -> conflict(e, request))

                        .onErrorResume(Exception.class, e -> oops(e, request));
    }

    private Mono<ServerResponse> paymentNofFound(RuntimeException e, ServerRequest request) {
        logException(e);
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).render("payment-not-found");
    }

    private Mono<ServerResponse> insufficientFunds(PaymentBalanceException e, ServerRequest request) {
        logException(e);

        return itemService.getCartWithoutPayments()
                .flatMap(cart -> ServerResponse
                        .status(HttpStatus.CONFLICT)
                        .render("cart", Map.of(
                                "items", cart.items(),
                                "total", cart.total(),
                                "isActive", false
                        )));
    }

    private Mono<ServerResponse> paymentServiceUnavailable(RuntimeException e, ServerRequest request) {
        logException(e);

        return itemService.getCartWithoutPayments()
                .flatMap(cart -> ServerResponse
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .render("cart", Map.of(
                                "items", cart.items(),
                                "total", cart.total(),
                                "isActive", false,
                                "message", "Сервис платежей не доступен. Попробуйте позже."
                        )));
    }

    private Mono<ServerResponse> oops(Exception e, ServerRequest request) {
        log.error("Handled exception of type {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).render("oops");
    }

    private Mono<ServerResponse> badRequest(Exception e, ServerRequest request) {
        logException(e);
        return ServerResponse.status(HttpStatus.BAD_REQUEST).render("bad-request");
    }

    private Mono<ServerResponse> adminBadRequest(Exception e, ServerRequest request) {
        logException(e);
        return ServerResponse.status(HttpStatus.BAD_REQUEST).render("admin-error");
    }

    private Mono<ServerResponse> notFound(NotFoundExceptionAbstract e, ServerRequest request) {
        logException(e);
        return ServerResponse.status(HttpStatus.NOT_FOUND)
                .render("not-found", Map.of("id", String.valueOf(e.getId())));
    }

    private Mono<ServerResponse> conflict(Exception e, ServerRequest request) {
        logException(e);
        return ServerResponse.status(HttpStatus.CONFLICT).render("conflict");
    }

    private void logException(Exception e) {
        log.warn("Handled exception of type {}: {}", e.getClass().getSimpleName(), e.getMessage());
    }

}
