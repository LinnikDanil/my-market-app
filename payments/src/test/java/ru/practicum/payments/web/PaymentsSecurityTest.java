package ru.practicum.payments.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.practicum.payments.api.DefaultApiController;
import ru.practicum.payments.config.SecurityConfig;
import ru.practicum.payments.domain.HoldRs;
import ru.practicum.payments.service.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = DefaultApiController.class)
@Import({SecurityConfig.class, PaymentDelegateImpl.class, DefaultExceptionHandler.class})
@TestPropertySource(properties = {
        "KEYCLOAK_ROLES_CLIENT_ID=payments-api",
        "KEYCLOAK_ISSUER_URI=http://issuer.example"
})
@DisplayName("Payments security")
class PaymentsSecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("unauthenticated request returns 401")
    void shouldReturnUnauthorizedWithoutToken() {
        webTestClient.get()
                .uri("/api/payments/balance/{userId}", 1L)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("authenticated request without SERVICE authority returns 403")
    void shouldReturnForbiddenWithoutServiceAuthority() {
        webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("USER")))
                .get()
                .uri("/api/payments/balance/{userId}", 1L)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("SERVICE authority can access balance endpoint")
    void shouldAllowBalanceWithServiceAuthority() {
        when(paymentService.getBalance(1L)).thenReturn(Mono.just(BigDecimal.valueOf(700)));

        webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("SERVICE")))
                .get()
                .uri("/api/payments/balance/{userId}", 1L)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(700);
    }

    @Test
    @DisplayName("csrf is disabled for API POST when SERVICE authority is present")
    void shouldAllowPostWithoutCsrf() {
        var paymentId = UUID.randomUUID();
        when(paymentService.holdPayment(eq(1L), any())).thenReturn(Mono.just(new HoldRs(paymentId)));

        webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("SERVICE")))
                .post()
                .uri("/api/payments/hold/{userId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":150}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paymentId").isEqualTo(paymentId.toString());
    }
}
