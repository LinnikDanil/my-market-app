package ru.practicum.market.integration;

import reactor.core.publisher.Mono;
import ru.practicum.market.integration.dto.Balance;
import ru.practicum.market.integration.dto.HoldRq;
import ru.practicum.market.integration.dto.HoldRs;

import java.util.UUID;

public interface PaymentAdapter {

    Mono<Balance> getBalance();

    Mono<HoldRs> hold(HoldRq holdRq);

    Mono<Void> confirm(UUID paymentId);

    Mono<Void> cancel(UUID paymentId);
}
