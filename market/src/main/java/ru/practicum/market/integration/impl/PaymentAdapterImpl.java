package ru.practicum.market.integration.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.practicum.market.integration.PaymentAdapter;
import ru.practicum.market.integration.exception.PaymentBalanceException;
import ru.practicum.market.integration.exception.PaymentIdNotFoundException;
import ru.practicum.market.integration.exception.PaymentServiceUnavailableException;
import ru.practicum.payments.api.DefaultApi;
import ru.practicum.payments.integration.domain.Balance;
import ru.practicum.payments.integration.domain.HoldRq;
import ru.practicum.payments.integration.domain.HoldRs;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentAdapterImpl implements PaymentAdapter {
    private static final String DEFAULT_SERVICE_ERROR = "Payment service error";

    private final DefaultApi paymentsApi;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Balance> getBalance(long userId) {
        return paymentsApi.getBalance(userId)
                .onErrorMap(this::mapError);
    }

    @Override
    public Mono<HoldRs> hold(long userId, HoldRq holdRq) {
        return paymentsApi.holdPayment(userId, holdRq)
                .onErrorMap(this::mapError);
    }

    @Override
    public Mono<Void> confirm(UUID paymentId) {
        return paymentsApi.confirmPayment(paymentId)
                .onErrorMap(this::mapError);
    }

    @Override
    public Mono<Void> cancel(UUID paymentId) {
        return paymentsApi.cancelPayment(paymentId)
                .onErrorMap(this::mapError);
    }

    private Throwable mapError(Throwable error) {
        if (error instanceof WebClientRequestException requestException) {
            return new PaymentServiceUnavailableException(requestException.getMessage());
        }

        if (!(error instanceof WebClientResponseException responseException)) {
            return error;
        }

        var message = extractMessage(responseException);
        HttpStatusCode statusCode = responseException.getStatusCode();

        if (statusCode.value() == HttpStatus.CONFLICT.value()) {
            return new PaymentBalanceException(message);
        }

        if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
            return new PaymentIdNotFoundException(message);
        }

        if (statusCode.is5xxServerError() || statusCode.value() == HttpStatus.UNAUTHORIZED.value()
                || statusCode.value() == HttpStatus.FORBIDDEN.value()) {
            return new PaymentServiceUnavailableException(message);
        }

        return responseException;
    }

    private String extractMessage(WebClientResponseException exception) {
        var body = exception.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return defaultMessageFor(exception);
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode messageNode = root.get("message");
            if (messageNode != null && !messageNode.isNull() && !messageNode.asText().isBlank()) {
                return messageNode.asText();
            }
        } catch (IOException ignored) {
        }

        return defaultMessageFor(exception);
    }

    private String defaultMessageFor(WebClientResponseException exception) {
        if (exception.getStatusCode().is5xxServerError()) {
            return DEFAULT_SERVICE_ERROR;
        }
        return exception.getStatusText();
    }
}
