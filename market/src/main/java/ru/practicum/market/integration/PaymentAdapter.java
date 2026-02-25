package ru.practicum.market.integration;

import reactor.core.publisher.Mono;
import ru.practicum.market.integration.dto.Balance;
import ru.practicum.market.integration.dto.HoldRq;
import ru.practicum.market.integration.dto.HoldRs;

import java.util.UUID;

/**
 * Адаптер для вызовов внешнего сервиса платежей.
 */
public interface PaymentAdapter {

    /**
     * Запрашивает текущий баланс.
     *
     * @return DTO баланса
     */
    Mono<Balance> getBalance();

    /**
     * Резервирует сумму (hold) под будущую оплату.
     *
     * @param holdRq параметры резерва
     * @return идентификатор резерва
     */
    Mono<HoldRs> hold(HoldRq holdRq);

    /**
     * Подтверждает ранее созданный резерв.
     *
     * @param paymentId идентификатор платежного резерва
     * @return сигнал завершения
     */
    Mono<Void> confirm(UUID paymentId);

    /**
     * Отменяет ранее созданный резерв.
     *
     * @param paymentId идентификатор платежного резерва
     * @return сигнал завершения
     */
    Mono<Void> cancel(UUID paymentId);
}
