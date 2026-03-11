package ru.practicum.payments.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import ru.practicum.payments.domain.HoldRq;
import ru.practicum.payments.domain.HoldRs;
import ru.practicum.payments.domain.Payment;
import ru.practicum.payments.service.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentDelegateImpl")
class PaymentDelegateImplTest {
    private static final Long USER_ID = 1L;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentDelegateImpl paymentDelegate;

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("ok")
        void test1() {
            when(paymentService.getBalance(USER_ID)).thenReturn(Mono.just(BigDecimal.valueOf(700)));

            var response = paymentDelegate.getBalance(USER_ID, null).block();

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getBalance()).isEqualTo(BigDecimal.valueOf(700));
        }
    }

    @Nested
    @DisplayName("replenishBalance")
    class ReplenishBalance {

        @Test
        @DisplayName("ok")
        void test1() {
            var payment = new Payment().amount(BigDecimal.valueOf(150));
            when(paymentService.replenishBalance(eq(USER_ID), any())).thenReturn(Mono.empty());

            ResponseEntity<Void> response = paymentDelegate.replenishBalance(USER_ID, Mono.just(payment), null).block();

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            verify(paymentService).replenishBalance(eq(USER_ID), any());
        }
    }

    @Nested
    @DisplayName("holdPayment")
    class HoldPayment {

        @Test
        @DisplayName("ok")
        void test1() {
            var holdRq = new HoldRq().amount(BigDecimal.valueOf(400));
            var holdRs = new HoldRs(UUID.randomUUID());
            when(paymentService.holdPayment(eq(USER_ID), any())).thenReturn(Mono.just(holdRs));

            var response = paymentDelegate.holdPayment(USER_ID, Mono.just(holdRq), null).block();

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPaymentId()).isEqualTo(holdRs.getPaymentId());
        }
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPayment {

        @Test
        @DisplayName("ok")
        void test1() {
            var paymentId = UUID.randomUUID();
            when(paymentService.confirmPayment(eq(paymentId))).thenReturn(Mono.empty());

            ResponseEntity<Void> response = paymentDelegate.confirmPayment(paymentId, null).block();

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Nested
    @DisplayName("cancelPayment")
    class CancelPayment {

        @Test
        @DisplayName("ok")
        void test1() {
            var paymentId = UUID.randomUUID();
            when(paymentService.cancelPayment(eq(paymentId))).thenReturn(Mono.empty());

            ResponseEntity<Void> response = paymentDelegate.cancelPayment(paymentId, null).block();

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }
}
