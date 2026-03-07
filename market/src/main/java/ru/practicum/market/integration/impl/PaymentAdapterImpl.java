package ru.practicum.market.integration.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Реактивный адаптер для вызова платежного API и нормализации ошибок интеграции.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAdapterImpl implements PaymentAdapter {
    private static final String DEFAULT_SERVICE_ERROR = "Payment service error";

    private final DefaultApi paymentsApi;
    private final ObjectMapper objectMapper;

    /**
     * Запрашивает текущий баланс пользователя в платежном сервисе.
     *
     * @param userId идентификатор пользователя
     * @return DTO баланса
     */
    @Override
    public Mono<Balance> getBalance(long userId) {
        log.debug("Requesting payment balance for userId={}", userId);
        return paymentsApi.getBalance(userId)
                .onErrorMap(this::mapError);
    }

    /**
     * Создаёт hold-операцию в платежном сервисе.
     *
     * @param userId идентификатор пользователя
     * @param holdRq параметры hold-операции
     * @return DTO результата hold-операции
     */
    @Override
    public Mono<HoldRs> hold(long userId, HoldRq holdRq) {
        log.debug("Requesting payment hold for userId={}, amount={}", userId, holdRq.getAmount());
        return paymentsApi.holdPayment(userId, holdRq)
                .onErrorMap(this::mapError);
    }

    /**
     * Подтверждает ранее созданную hold-операцию.
     *
     * @param paymentId идентификатор hold-операции
     * @return сигнал завершения
     */
    @Override
    public Mono<Void> confirm(UUID paymentId) {
        log.debug("Confirming payment hold paymentId={}", paymentId);
        return paymentsApi.confirmPayment(paymentId)
                .onErrorMap(this::mapError);
    }

    /**
     * Отменяет ранее созданную hold-операцию.
     *
     * @param paymentId идентификатор hold-операции
     * @return сигнал завершения
     */
    @Override
    public Mono<Void> cancel(UUID paymentId) {
        log.debug("Cancelling payment hold paymentId={}", paymentId);
        return paymentsApi.cancelPayment(paymentId)
                .onErrorMap(this::mapError);
    }

    /**
     * Преобразует ошибки WebClient в доменные исключения интеграционного слоя.
     *
     * @param error исходная ошибка клиента
     * @return нормализованное исключение
     */
    private Throwable mapError(Throwable error) {
        if (error instanceof WebClientRequestException requestException) {
            log.warn("Payment service is unavailable: {}", requestException.getMessage());
            return new PaymentServiceUnavailableException(requestException.getMessage());
        }

        if (!(error instanceof WebClientResponseException responseException)) {
            return error;
        }

        var message = extractMessage(responseException);
        HttpStatusCode statusCode = responseException.getStatusCode();

        if (statusCode.value() == HttpStatus.CONFLICT.value()) {
            log.warn("Payment conflict from service: {}", message);
            return new PaymentBalanceException(message);
        }

        if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
            log.warn("Payment entity not found in service: {}", message);
            return new PaymentIdNotFoundException(message);
        }

        if (statusCode.is5xxServerError() || statusCode.value() == HttpStatus.UNAUTHORIZED.value()
                || statusCode.value() == HttpStatus.FORBIDDEN.value()) {
            log.warn("Payment service returned unavailable status {}: {}", statusCode.value(), message);
            return new PaymentServiceUnavailableException(message);
        }

        return responseException;
    }

    /**
     * Извлекает текст ошибки из payload ответа платежного сервиса.
     *
     * @param exception исключение WebClient
     * @return текст ошибки
     */
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

    /**
     * Возвращает fallback-сообщение для ошибок платежного сервиса.
     *
     * @param exception исключение WebClient
     * @return fallback-сообщение
     */
    private String defaultMessageFor(WebClientResponseException exception) {
        if (exception.getStatusCode().is5xxServerError()) {
            return DEFAULT_SERVICE_ERROR;
        }
        return exception.getStatusText();
    }
}
