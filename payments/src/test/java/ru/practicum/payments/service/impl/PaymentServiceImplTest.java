package ru.practicum.payments.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.practicum.payments.domain.HoldRq;
import ru.practicum.payments.domain.Payment;
import ru.practicum.payments.exception.PaymentBalanceException;
import ru.practicum.payments.exception.PaymentNotFoundException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DisplayName("PaymentServiceImpl")
class PaymentServiceImplTest {

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl();
    }

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("default balance")
        void test1() {
            var balance = paymentService.getBalance().block();
            assertThat(balance).isEqualTo(BigDecimal.valueOf(5000));
        }
    }

    @Nested
    @DisplayName("replenishBalance")
    class ReplenishBalance {

        @Test
        @DisplayName("increase balance")
        void test1() {
            var payment = new Payment().amount(BigDecimal.valueOf(200));

            paymentService.replenishBalance(Mono.just(payment)).block();

            var balance = paymentService.getBalance().block();
            assertThat(balance).isEqualTo(BigDecimal.valueOf(5200));
        }
    }

    @Nested
    @DisplayName("holdPayment")
    class HoldPayment {

        @Test
        @DisplayName("success")
        void test1() {
            var hold = paymentService.holdPayment(Mono.just(new HoldRq().amount(BigDecimal.valueOf(1000)))).block();

            assertThat(hold).isNotNull();
            assertThat(hold.getPaymentId()).isNotNull();
            assertThat(paymentService.getBalance().block()).isEqualTo(BigDecimal.valueOf(4000));
        }

        @Test
        @DisplayName("insufficient funds")
        void test2() {
            assertThatExceptionOfType(PaymentBalanceException.class)
                    .isThrownBy(() -> paymentService.holdPayment(
                            Mono.just(new HoldRq().amount(BigDecimal.valueOf(6000)))
                    ).block());
        }
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPayment {

        @Test
        @DisplayName("remove hold")
        void test1() {
            var hold = paymentService.holdPayment(Mono.just(new HoldRq().amount(BigDecimal.valueOf(1000)))).block();

            paymentService.confirmPayment(hold.getPaymentId()).block();

            assertThat(paymentService.getBalance().block()).isEqualTo(BigDecimal.valueOf(4000));
            assertThatExceptionOfType(PaymentNotFoundException.class)
                    .isThrownBy(() -> paymentService.confirmPayment(hold.getPaymentId()).block());
        }
    }

    @Nested
    @DisplayName("cancelPayment")
    class CancelPayment {

        @Test
        @DisplayName("return balance")
        void test1() {
            var hold = paymentService.holdPayment(Mono.just(new HoldRq().amount(BigDecimal.valueOf(1000)))).block();

            paymentService.cancelPayment(hold.getPaymentId()).block();

            assertThat(paymentService.getBalance().block()).isEqualTo(BigDecimal.valueOf(5000));
            assertThatExceptionOfType(PaymentNotFoundException.class)
                    .isThrownBy(() -> paymentService.cancelPayment(hold.getPaymentId()).block());
        }
    }
}
