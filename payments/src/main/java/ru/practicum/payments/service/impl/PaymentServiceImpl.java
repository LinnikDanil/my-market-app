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

/**
 * Учебная реализация сервиса платежей с двухфазной схемой списания: hold → confirm / cancel.
 *
 * <h2>Модель</h2>
 * <ul>
 *   <li>У каждого пользователя свой баланс, определяемый по {@code userId}.</li>
 *   <li>Баланс хранится в памяти процесса и не переживает перезапуск приложения.</li>
 *   <li>Поддерживаются "заморозки" (holds) — временные резервы сумм под списание.</li>
 * </ul>
 *
 * <h2>Двухфазная схема</h2>
 * <ul>
 *   <li>{@link PaymentService#holdPayment(Long, Mono)}: резервирует сумму и уменьшает доступный баланс.
 *       Возвращает {@code paymentId}, по которому операция может быть подтверждена или отменена.</li>
 *   <li>{@link #confirmPayment(UUID)}: подтверждает ранее созданный hold и завершает операцию.
 *       В текущей реализации дополнительного списания не происходит, т.к. баланс уже уменьшен на этапе hold.</li>
 *   <li>{@link #cancelPayment(UUID)}: отменяет hold и возвращает зарезервированную сумму обратно на баланс.</li>
 * </ul>
 *
 *
 * <h2>Ограничения учебной реализации</h2>
 * <ul>
 *   <li>Нет хранения в БД, только in-memory состояние процесса.</li>
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
    private static final BigDecimal DEFAULT_BALANCE = BigDecimal.valueOf(5000);

    private final ConcurrentHashMap<Long, BigDecimal> userBalances;
    private final ConcurrentHashMap<UUID, HoldOperation> holds;

    private record HoldOperation(Long userId, BigDecimal amount) {
    }

    /**
     * Инициализирует сервис стартовым балансом и пустым набором hold-операций.
     */
    public PaymentServiceImpl() {
        userBalances = new ConcurrentHashMap<>();
        holds = new ConcurrentHashMap<>();
    }

    /**
     * Возвращает текущий баланс.
     */
    @Override
    public Mono<BigDecimal> getBalance(Long userId) {
        var currentBalance = userBalances.computeIfAbsent(userId, ignored -> DEFAULT_BALANCE);
        log.debug("Getting balance for user {} = {}.", userId, currentBalance);
        return Mono.just(currentBalance);
    }

    /**
     * Пополняет баланс на сумму из запроса.
     */
    @Override
    public Mono<Void> replenishBalance(Long userId, Mono<Payment> paymentMono) {
        return paymentMono
                .map(Payment::getAmount)
                .doOnNext(amount -> {
                    log.debug("Replenish user {} balance at {}.", userId, amount);
                    var newBalance = userBalances.compute(userId, (id, current) -> {
                        var existingBalance = current == null ? DEFAULT_BALANCE : current;
                        return existingBalance.add(amount);
                    });
                    log.debug("New user {} balance at {}.", userId, newBalance);
                })
                .then();
    }

    /**
     * Резервирует сумму на оплату, уменьшая доступный баланс.
     */
    @Override
    public Mono<HoldRs> holdPayment(Long userId, Mono<HoldRq> paymentMono) {
        return paymentMono.flatMap(rq ->
                Mono.fromCallable(() -> {
                    var amount = rq.getAmount();

                    // Списываем, пока не спишется или не вылетит ошибка
                    while (true) {
                        var currentBalance = userBalances.computeIfAbsent(userId, ignored -> DEFAULT_BALANCE);
                        log.debug("Try Hold payment for user {} on amount = {}, balance = {}.", userId, amount, currentBalance);
                        if (currentBalance.compareTo(amount) < 0) {
                            throw new PaymentBalanceException(amount, "There are not enough funds to make the payment.");
                        }
                        var newBalance = currentBalance.subtract(amount);
                        // Если никто другой не изменил текущий баланс, завершаем метод.
                        if (userBalances.replace(userId, currentBalance, newBalance)) {
                            var paymentId = UUID.randomUUID();
                            holds.put(paymentId, new HoldOperation(userId, amount));
                            log.debug("SUCCESSFUL Hold payment for user {} on amount = {}, new balance = {}.",
                                    userId, amount, newBalance);
                            return new HoldRs(paymentId);
                        }
                    }
                })
        );
    }

    /**
     * Подтверждает hold-операцию.
     */
    @Override
    public Mono<Void> confirmPayment(UUID paymentId) {
        log.debug("Confirm payment id = {}.", paymentId);

        return Mono.fromRunnable(() -> {
            var removed = holds.remove(paymentId);
            if (removed == null) {
                throwAndLog(paymentId);
            }
            log.debug("Confirmed hold with id = {} for user {}.", paymentId, removed.userId());
        });
    }


    /**
     * Отменяет hold-операцию и возвращает сумму в баланс.
     */
    @Override
    public Mono<Void> cancelPayment(UUID paymentId) {
        log.debug("Cancel payment id = {}.", paymentId);

        return Mono.fromRunnable(() -> {
            var hold = holds.remove(paymentId);
            if (hold == null) {
                throwAndLog(paymentId);
            }
            var newBalance = userBalances.compute(hold.userId(), (id, current) -> {
                var existingBalance = current == null ? DEFAULT_BALANCE : current;
                return existingBalance.add(hold.amount());
            });

            log.debug("Cancelled hold with id = {} for user {}. New balance = {}.",
                    paymentId, hold.userId(), newBalance);
        });
    }

    /**
     * Логирует и бросает исключение, если hold-операция не найдена.
     */
    private void throwAndLog(UUID paymentId) {
        log.error("Hold with id = {} was not found.", paymentId);
        throw new PaymentNotFoundException(
                "Hold with payment id = %s not found".formatted(paymentId)
        );
    }
}
