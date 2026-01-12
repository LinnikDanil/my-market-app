package ru.practicum.market.integration.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.practicum.market.integration.PaymentAdapter;
import ru.practicum.market.integration.dto.Balance;
import ru.practicum.market.integration.dto.ErrorResponse;
import ru.practicum.market.integration.dto.HoldRq;
import ru.practicum.market.integration.dto.HoldRs;
import ru.practicum.market.integration.exception.PaymentBalanceException;
import ru.practicum.market.integration.exception.PaymentIdNotFoundException;
import ru.practicum.market.integration.exception.PaymentServiceUnavailableException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static ru.practicum.market.integration.util.PaymentUris.PAYMENT_BALANCE;
import static ru.practicum.market.integration.util.PaymentUris.PAYMENT_CANCEL;
import static ru.practicum.market.integration.util.PaymentUris.PAYMENT_CONFIRM;
import static ru.practicum.market.integration.util.PaymentUris.PAYMENT_HOLD;

@Service
@RequiredArgsConstructor
public class PaymentAdapterImpl implements PaymentAdapter {
    private final WebClient webClient;

    @Override
    public Mono<Balance> getBalance() {
        return webClient.get()
                .uri(PAYMENT_BALANCE)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(ErrorResponse.class)
                                .defaultIfEmpty(new ErrorResponse(
                                        "PAYMENT_SERVICE_ERROR",
                                        "Payment service error",
                                        OffsetDateTime.now())
                                )
                                .flatMap(err -> Mono.error(
                                        new PaymentServiceUnavailableException(err.message())
                                ))
                )
                .bodyToMono(Balance.class);
    }

    @Override
    public Mono<HoldRs> hold(HoldRq holdRq) {
        return webClient.post()
                .uri(PAYMENT_HOLD)
                .bodyValue(holdRq)
                .retrieve()
                .onStatus(
                        status -> status.value() == HttpStatus.CONFLICT.value(),
                        response -> response.bodyToMono(ErrorResponse.class)
                                .flatMap(err -> Mono.error(
                                        new PaymentBalanceException(err.message())
                                ))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(ErrorResponse.class)
                                .defaultIfEmpty(new ErrorResponse(
                                        "PAYMENT_SERVICE_ERROR",
                                        "Payment service error",
                                        OffsetDateTime.now())
                                )
                                .flatMap(err -> Mono.error(
                                        new PaymentServiceUnavailableException(err.message())
                                ))
                )
                .bodyToMono(HoldRs.class);
    }

    @Override
    public Mono<Void> confirm(UUID paymentId) {
        return exchangeMethod(paymentId, PAYMENT_CONFIRM);
    }

    @Override
    public Mono<Void> cancel(UUID paymentId) {
        return exchangeMethod(paymentId, PAYMENT_CANCEL);
    }

    private Mono<Void> exchangeMethod(UUID paymentId, String uri) {
        return webClient.post()
                .uri(uri + paymentId)
                .retrieve()
                .onStatus(
                        status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        response -> response.bodyToMono(ErrorResponse.class)
                                .flatMap(err -> Mono.error(
                                        new PaymentIdNotFoundException(err.message())
                                ))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(ErrorResponse.class)
                                .defaultIfEmpty(new ErrorResponse(
                                        "PAYMENT_SERVICE_ERROR",
                                        "Payment service error",
                                        OffsetDateTime.now())
                                )
                                .flatMap(err -> Mono.error(
                                        new PaymentServiceUnavailableException(err.message())
                                ))
                )
                .toBodilessEntity()
                .then();
    }
}
