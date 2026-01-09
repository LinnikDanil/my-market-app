package ru.practicum.payments.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.practicum.payments.api.DefaultApiDelegate;
import ru.practicum.payments.domain.Balance;
import ru.practicum.payments.domain.Payment;
import ru.practicum.payments.service.PaymentService;

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
    public Mono<ResponseEntity<Void>> postBalance(Mono<Payment> payment, ServerWebExchange exchange) {
        return paymentService.replenishBalance(payment)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().build()));
    }

    @Override
    public Mono<ResponseEntity<Void>> postPayment(Mono<Payment> payment, ServerWebExchange exchange) {
        return paymentService.makePayment(payment)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().build()));
    }
}
