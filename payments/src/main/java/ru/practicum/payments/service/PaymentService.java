package ru.practicum.payments.service;

import reactor.core.publisher.Mono;
import ru.practicum.payments.domain.HoldRq;
import ru.practicum.payments.domain.HoldRs;
import ru.practicum.payments.domain.Payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {

    Mono<BigDecimal> getBalance();

    Mono<Void> replenishBalance(Mono<Payment> payment);

    Mono<HoldRs> holdPayment(Mono<HoldRq> payment);

    Mono<Void> confirmPayment(UUID paymentId);

    Mono<Void> cancelPayment(UUID paymentId);
}
