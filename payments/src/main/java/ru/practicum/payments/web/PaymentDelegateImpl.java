package ru.practicum.payments.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
     *
     * @param userId   идентификатор пользователя
     * @param exchange текущий web-exchange
     * @return HTTP-ответ с текущим балансом
     */
    @Override
    @PreAuthorize("hasAuthority('SERVICE')")
    public Mono<ResponseEntity<Balance>> getBalance(Long userId, ServerWebExchange exchange) {
        log.debug("API getBalance for userId={}", userId);
        return paymentService.getBalance(userId)
                .map(balance ->
                        ResponseEntity.ok(
                                new Balance(balance)
                        )
                );
    }

    /**
     * Обрабатывает API-запрос пополнения баланса.
     *
     * @param userId   идентификатор пользователя
     * @param payment  тело запроса на пополнение
     * @param exchange текущий web-exchange
     * @return HTTP-ответ о завершении операции
     */
    @Override
    @PreAuthorize("hasAuthority('SERVICE')")
    public Mono<ResponseEntity<Void>> replenishBalance(Long userId, Mono<Payment> payment, ServerWebExchange exchange) {
        log.info("API replenishBalance for userId={}", userId);
        return paymentService.replenishBalance(userId, payment)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().build()));
    }

    /**
     * Обрабатывает API-запрос на создание hold-операции.
     *
     * @param userId   идентификатор пользователя
     * @param holdRq   тело запроса hold
     * @param exchange текущий web-exchange
     * @return HTTP-ответ с идентификатором hold-операции
     */
    @Override
    @PreAuthorize("hasAuthority('SERVICE')")
    public Mono<ResponseEntity<HoldRs>> holdPayment(Long userId, Mono<HoldRq> holdRq, ServerWebExchange exchange) {
        log.info("API holdPayment for userId={}", userId);
        return paymentService.holdPayment(userId, holdRq)
                .map(ResponseEntity::ok);
    }

    /**
     * Обрабатывает API-запрос на подтверждение hold-операции.
     *
     * @param paymentId идентификатор hold-операции
     * @param exchange  текущий web-exchange
     * @return HTTP-ответ о завершении операции
     */
    @Override
    @PreAuthorize("hasAuthority('SERVICE')")
    public Mono<ResponseEntity<Void>> confirmPayment(UUID paymentId, ServerWebExchange exchange) {
        log.info("API confirmPayment for paymentId={}", paymentId);
        return paymentService.confirmPayment(paymentId)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().build()));
    }

    /**
     * Обрабатывает API-запрос на отмену hold-операции.
     *
     * @param paymentId идентификатор hold-операции
     * @param exchange  текущий web-exchange
     * @return HTTP-ответ о завершении операции
     */
    @Override
    @PreAuthorize("hasAuthority('SERVICE')")
    public Mono<ResponseEntity<Void>> cancelPayment(UUID paymentId, ServerWebExchange exchange) {
        log.info("API cancelPayment for paymentId={}", paymentId);
        return paymentService.cancelPayment(paymentId)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().build()));
    }
}
