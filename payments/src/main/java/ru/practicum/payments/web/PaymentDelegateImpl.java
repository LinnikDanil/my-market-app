package ru.practicum.payments.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.practicum.payments.api.DefaultApiDelegate;
import ru.practicum.payments.domain.Balance;
import ru.practicum.payments.domain.HoldRq;
import ru.practicum.payments.domain.HoldRs;
import ru.practicum.payments.domain.Payment;
import ru.practicum.payments.service.PaymentService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentDelegateImpl implements DefaultApiDelegate {

    private final PaymentService paymentService;

    /**
     * Возвращает текущий баланс в формате API-ответа.
     */
    @Override
    public Mono<ResponseEntity<Balance>> getBalance(ServerWebExchange exchange) {
        return paymentService.getBalance()
                .map(balance ->
                        ResponseEntity.ok(
                                new Balance(balance)
                        )
                );
    }

    /**
     * Обрабатывает API-запрос пополнения баланса.
     */
    @Override
    public Mono<ResponseEntity<Void>> replenishBalance(Mono<Payment> payment, ServerWebExchange exchange) {
        return paymentService.replenishBalance(payment)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().build()));
    }

    /**
     * Обрабатывает API-запрос на создание hold-операции.
     */
    @Override
    public Mono<ResponseEntity<HoldRs>> holdPayment(Mono<HoldRq> holdRq, ServerWebExchange exchange) {
        return paymentService.holdPayment(holdRq)
                .map(ResponseEntity::ok);
    }

    /**
     * Обрабатывает API-запрос на подтверждение hold-операции.
     */
    @Override
    public Mono<ResponseEntity<Void>> confirmPayment(UUID paymentId, ServerWebExchange exchange) {
        return paymentService.confirmPayment(paymentId)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().build()));
    }

    /**
     * Обрабатывает API-запрос на отмену hold-операции.
     */
    @Override
    public Mono<ResponseEntity<Void>> cancelPayment(UUID paymentId, ServerWebExchange exchange) {
        return paymentService.cancelPayment(paymentId)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().build()));
    }
}
