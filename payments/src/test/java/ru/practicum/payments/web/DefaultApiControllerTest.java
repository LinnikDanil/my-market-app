package ru.practicum.payments.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.practicum.payments.api.DefaultApiController;
import ru.practicum.payments.api.DefaultApiDelegate;
import ru.practicum.payments.config.SecurityConfig;
import ru.practicum.payments.domain.Balance;
import ru.practicum.payments.domain.HoldRq;
import ru.practicum.payments.domain.HoldRs;
import ru.practicum.payments.domain.Payment;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(
        controllers = DefaultApiController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientWebSecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration.class
        }
)
@Import(DefaultExceptionHandler.class)
@DisplayName("DefaultApiController")
class DefaultApiControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DefaultApiDelegate defaultApiDelegate;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("ok")
        void test1() {
            when(defaultApiDelegate.getBalance(anyLong(), any()))
                    .thenReturn(Mono.just(ResponseEntity.ok(new Balance(BigDecimal.valueOf(1000)))));

            webTestClient.mutateWith(mockJwt()).get()
                    .uri("/api/payments/balance/{userId}", 1L)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.balance").isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("replenishBalance")
    class ReplenishBalance {

        @Test
        @DisplayName("ok")
        void test1() {
            when(defaultApiDelegate.replenishBalance(anyLong(), any(), any()))
                    .thenReturn(Mono.just(ResponseEntity.ok().build()));

            webTestClient.mutateWith(mockJwt()).post()
                    .uri("/api/payments/balance/{userId}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new Payment().amount(BigDecimal.valueOf(250)))
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("holdPayment")
    class HoldPayment {

        @Test
        @DisplayName("ok")
        void test1() {
            var paymentId = UUID.randomUUID();
            when(defaultApiDelegate.holdPayment(anyLong(), any(), any()))
                    .thenReturn(Mono.just(ResponseEntity.ok(new HoldRs(paymentId))));

            webTestClient.mutateWith(mockJwt()).post()
                    .uri("/api/payments/hold/{userId}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new HoldRq().amount(BigDecimal.valueOf(350)))
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.paymentId").isEqualTo(paymentId.toString());
        }
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPayment {

        @Test
        @DisplayName("ok")
        void test1() {
            var paymentId = UUID.randomUUID();
            when(defaultApiDelegate.confirmPayment(eq(paymentId), any()))
                    .thenReturn(Mono.just(ResponseEntity.ok().build()));

            webTestClient.mutateWith(mockJwt()).post()
                    .uri("/api/payments/confirm/{paymentId}", paymentId)
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("cancelPayment")
    class CancelPayment {

        @Test
        @DisplayName("ok")
        void test1() {
            var paymentId = UUID.randomUUID();
            when(defaultApiDelegate.cancelPayment(eq(paymentId), any()))
                    .thenReturn(Mono.just(ResponseEntity.ok().build()));

            webTestClient.mutateWith(mockJwt()).post()
                    .uri("/api/payments/cancel/{paymentId}", paymentId)
                    .exchange()
                    .expectStatus().isOk();
        }
    }
}
