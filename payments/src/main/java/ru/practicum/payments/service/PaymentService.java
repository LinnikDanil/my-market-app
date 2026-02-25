package ru.practicum.payments.service;

import reactor.core.publisher.Mono;
import ru.practicum.payments.domain.HoldRq;
import ru.practicum.payments.domain.HoldRs;
import ru.practicum.payments.domain.Payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Сервис бизнес-операций платежного модуля.
 */
public interface PaymentService {

    /**
     * Возвращает текущий доступный баланс.
     *
     * @return сумма баланса
     */
    Mono<BigDecimal> getBalance();

    /**
     * Пополняет баланс на указанную сумму.
     *
     * @param payment запрос с суммой пополнения
     * @return сигнал завершения
     */
    Mono<Void> replenishBalance(Mono<Payment> payment);

    /**
     * Резервирует сумму на оплату.
     *
     * @param payment запрос hold
     * @return результат hold с идентификатором
     */
    Mono<HoldRs> holdPayment(Mono<HoldRq> payment);

    /**
     * Подтверждает ранее созданный hold.
     *
     * @param paymentId идентификатор hold-операции
     * @return сигнал завершения
     */
    Mono<Void> confirmPayment(UUID paymentId);

    /**
     * Отменяет ранее созданный hold и возвращает сумму на баланс.
     *
     * @param paymentId идентификатор hold-операции
     * @return сигнал завершения
     */
    Mono<Void> cancelPayment(UUID paymentId);
}
