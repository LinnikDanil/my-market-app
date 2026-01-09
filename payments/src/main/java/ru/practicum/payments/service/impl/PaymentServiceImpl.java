package ru.practicum.payments.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.practicum.payments.domain.Payment;
import ru.practicum.payments.exception.PaymentBalanceException;
import ru.practicum.payments.service.PaymentService;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Учебная реализация сервиса платежей.
 *
 * <p>
 * Такой подход выбран исключительно в учебных целях для демонстрации работы витрины магазина.
 * </p>
 *
 * <p>
 * В рамках данного учебного проекта используется упрощённая модель:
 * <ul>
 *     <li>существует только один баланс;</li>
 *     <li>нет пользователей и идентификаторов аккаунтов;</li>
 *     <li>баланс хранится в памяти приложения.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>В реальном проекте</b> логика выглядела бы иначе:
 * <ul>
 *     <li>баланс был бы привязан к конкретному пользователю (userId);</li>
 *     <li>все операции выполнялись бы по идентификатору пользователя;</li>
 *     <li>данные хранились бы в базе данных;</li>
 *     <li>операции списания были бы атомарными и потокобезопасными;</li>
 *     <li>применялись бы транзакции и/или блокировки.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {
    private final AtomicReference<BigDecimal> balance;

    public PaymentServiceImpl() {
        log.info("Стартовый баланс установлен в 5000 рублей.");
        balance = new AtomicReference<>(BigDecimal.valueOf(5000));
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
    public Mono<Void> makePayment(Mono<Payment> paymentMono) {
        return paymentMono
                .flatMap(payment -> {
                    var amount = payment.getAmount();
                    log.debug("Make payment on {}, balance = {}.", amount, balance.get());

                    // Списываем, пока не спишется или не вылетит ошибка
                    while (true) {
                        var currentBalance = balance.get();

                        if (currentBalance.compareTo(amount) < 0) {
                            return Mono.error(
                                    new PaymentBalanceException(
                                            amount, "There are not enough funds to make the payment."
                                    )
                            );
                        }

                        var newBalance = currentBalance.subtract(amount);
                        // Если никто другой не изменил текущий баланс завершаем метод
                        if (balance.compareAndSet(currentBalance, newBalance)) {
                            return Mono.empty();
                        }
                    }
                });
    }
}
