package ru.practicum.payments.service;

import reactor.core.publisher.Mono;
import ru.practicum.payments.domain.Payment;

import java.math.BigDecimal;

public interface PaymentService {

    Mono<BigDecimal> getBalance();

    Mono<Void> replenishBalance(Mono<Payment> payment);

    Mono<Void> makePayment(Mono<Payment> payment);
}
