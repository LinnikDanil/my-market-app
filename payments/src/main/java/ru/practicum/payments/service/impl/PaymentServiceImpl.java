package ru.practicum.payments.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.practicum.payments.domain.HoldRq;
import ru.practicum.payments.domain.HoldRs;
import ru.practicum.payments.domain.Payment;
import ru.practicum.payments.exception.PaymentBalanceException;
import ru.practicum.payments.exception.PaymentNotFoundException;
import ru.practicum.payments.service.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Учебная реализация сервиса платежей с двухфазной схемой списания: hold → confirm / cancel.
 *
 * <h2>Модель</h2>
 * <ul>
 *   <li>В приложении существует один общий баланс (без пользователей и счетов).</li>
 *   <li>Баланс хранится в памяти процесса и не переживает перезапуск приложения.</li>
 *   <li>Поддерживаются "заморозки" (holds) — временные резервы сумм под списание.</li>
 * </ul>
 *
 * <h2>Двухфазная схема</h2>
 * <ul>
 *   <li>{@link #holdPayment(Mono)}: резервирует сумму и уменьшает доступный баланс.
 *       Возвращает {@code paymentId}, по которому операция может быть подтверждена или отменена.</li>
 *   <li>{@link #confirmPayment(UUID)}: подтверждает ранее созданный hold и завершает операцию.
 *       В текущей реализации дополнительного списания не происходит, т.к. баланс уже уменьшен на этапе hold.</li>
 *   <li>{@link #cancelPayment(UUID)}: отменяет hold и возвращает зарезервированную сумму обратно на баланс.</li>
 * </ul>
 *
 *
 * <h2>Ограничения учебной реализации</h2>
 * <ul>
 *   <li>Нет пользователей, счетов и привязки к {@code userId}.</li>
 *   <li>Нет срока жизни hold (TTL) и фоновой очистки "зависших" hold-ов.</li>
 *   <li>Нет статусов hold-ов (HELD/CONFIRMED/CANCELED), поэтому повторные confirm/cancel
 *       для одного {@code paymentId} будут приводить к ошибке "не найдено".</li>
 *   <li>Данные не сохраняются в БД и не обеспечивают консистентность в распределённой среде.</li>
 * </ul>
 *
 * <p><b>Примечание:</b> В реальном проекте для платежей я бы использовал идемпотентные ключи,
 * хранение операций в БД, статусы и механизмы согласования (например, Saga/Outbox).</p>
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {
    private final AtomicReference<BigDecimal> balance;
    private final ConcurrentHashMap<UUID, BigDecimal> holds;

    public PaymentServiceImpl() {
        log.info("Стартовый баланс установлен в 5000 рублей.");
        balance = new AtomicReference<>(BigDecimal.valueOf(5000));
        holds = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<BigDecimal> getBalance() {
        var currentBalance = balance.get();
        log.debug("Getting balance = {}.", currentBalance);
        return Mono.just(currentBalance);
    }

    @Override
    public Mono<Void> replenishBalance(Mono<Payment> paymentMono) {
        return paymentMono
                .map(Payment::getAmount)
                .doOnNext(amount -> {
                    log.debug("Replenish balance at {}.", amount);
                    var newBalance = balance.updateAndGet(b -> b.add(amount));
                    log.debug("New balance at {}.", newBalance);
                })
                .then();
    }

    @Override
    public Mono<HoldRs> holdPayment(Mono<HoldRq> paymentMono) {
        return paymentMono.flatMap(rq ->
                Mono.fromCallable(() -> {
                    var amount = rq.getAmount();

                    // Списываем, пока не спишется или не вылетит ошибка
                    while (true) {
                        var currentBalance = balance.get();
                        log.debug("Try Hold payment on amount = {}, balance = {}.", amount, currentBalance);
                        if (currentBalance.compareTo(amount) < 0) {
                            throw new PaymentBalanceException(amount, "There are not enough funds to make the payment.");
                        }
                        var newBalance = currentBalance.subtract(amount);
                        // Если никто другой не изменил текущий баланс завершаем метод
                        if (balance.compareAndSet(currentBalance, newBalance)) {
                            var paymentId = UUID.randomUUID();
                            holds.put(paymentId, amount);
                            log.debug("SUCCESSFUL Hold payment on amount = {}, new balance = {}.", amount, newBalance);
                            return new HoldRs(paymentId);
                        }
                    }
                })
        );
    }

    @Override
    public Mono<Void> confirmPayment(UUID paymentId) {
        log.debug("Confirm payment id = {}.", paymentId);

        return Mono.fromRunnable(() -> {
            var removed = holds.remove(paymentId);
            if (removed == null) {
                throwAndLog(paymentId);
            }
            log.debug("Confirmed hold with id = {} balance = {}.", paymentId, balance.get());
        });
    }


    @Override
    public Mono<Void> cancelPayment(UUID paymentId) {
        log.debug("Cancel payment id = {}.", paymentId);

        return Mono.fromRunnable(() -> {
            var amount = holds.remove(paymentId);
            if (amount == null) {
                throwAndLog(paymentId);
            }
            balance.updateAndGet(b -> b.add(amount));

            log.debug("Cancelled hold with id = {} balance = {}.", paymentId, balance.get());
        });
    }

    private void throwAndLog(UUID paymentId) {
        log.error("Hold with id = {} was not found.", paymentId);
        throw new PaymentNotFoundException(
                "Hold with payment id = %s not found".formatted(paymentId)
        );
    }
}
