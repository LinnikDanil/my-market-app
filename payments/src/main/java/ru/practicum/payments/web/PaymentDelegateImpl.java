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

    @Override
    public Mono<ResponseEntity<Balance>> getBalance(ServerWebExchange exchange) {
        return paymentService.getBalance()
                .map(balance ->
                        ResponseEntity.ok(
                                new Balance(balance)
                        )
                );
    }

    @Override
    public Mono<ResponseEntity<Void>> replenishBalance(Mono<Payment> payment, ServerWebExchange exchange) {
        return paymentService.replenishBalance(payment)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().build()));
    }

    @Override
    public Mono<ResponseEntity<HoldRs>> holdPayment(Mono<HoldRq> holdRq, ServerWebExchange exchange) {
        return paymentService.holdPayment(holdRq)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> confirmPayment(UUID paymentId, ServerWebExchange exchange) {
        return paymentService.confirmPayment(paymentId)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().build()));
    }

    @Override
    public Mono<ResponseEntity<Void>> cancelPayment(UUID paymentId, ServerWebExchange exchange) {
        return paymentService.cancelPayment(paymentId)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().build()));
    }
}
