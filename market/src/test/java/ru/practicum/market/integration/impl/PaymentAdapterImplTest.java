package ru.practicum.market.integration.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.integration.exception.PaymentBalanceException;
import ru.practicum.market.integration.exception.PaymentIdNotFoundException;
import ru.practicum.market.integration.exception.PaymentServiceUnavailableException;
import ru.practicum.payments.api.DefaultApi;
import ru.practicum.payments.integration.client.ApiClient;
import ru.practicum.payments.integration.domain.Balance;
import ru.practicum.payments.integration.domain.HoldRq;
import ru.practicum.payments.integration.domain.HoldRs;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DisplayName("PaymentAdapterImpl")
class PaymentAdapterImplTest {

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("returns balance")
        void test1() {
            var adapter = buildAdapter(responseWithBody(HttpStatus.OK, balanceJson(BigDecimal.valueOf(1200))));

            var balance = adapter.getBalance(1L).block();

            assertThat(balance).isEqualTo(new Balance().balance(BigDecimal.valueOf(1200)));
        }

        @Test
        @DisplayName("throws on service error")
        void test2() {
            var adapter = buildAdapter(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());

            assertThatExceptionOfType(PaymentServiceUnavailableException.class)
                    .isThrownBy(() -> adapter.getBalance(1L).block())
                    .withMessage("Payment service error");
        }
    }

    @Nested
    @DisplayName("hold")
    class Hold {

        @Test
        @DisplayName("returns payment id")
        void test1() {
            var paymentId = UUID.randomUUID();
            var adapter = buildAdapter(responseWithBody(HttpStatus.OK, holdResponseJson(paymentId)));

            var response = adapter.hold(1L, new HoldRq().amount(BigDecimal.valueOf(400))).block();

            assertThat(response).isEqualTo(new HoldRs().paymentId(paymentId));
        }

        @Test
        @DisplayName("throws on insufficient funds")
        void test2() {
            var adapter = buildAdapter(responseWithBody(
                    HttpStatus.CONFLICT,
                    errorResponseJson("Insufficient balance")
            ));

            assertThatExceptionOfType(PaymentBalanceException.class)
                    .isThrownBy(() -> adapter.hold(1L, new HoldRq().amount(BigDecimal.valueOf(400))).block())
                    .withMessage("Insufficient balance");
        }

        @Test
        @DisplayName("throws on service error")
        void test3() {
            var adapter = buildAdapter(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());

            assertThatExceptionOfType(PaymentServiceUnavailableException.class)
                    .isThrownBy(() -> adapter.hold(1L, new HoldRq().amount(BigDecimal.valueOf(400))).block())
                    .withMessage("Payment service error");
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("completes successfully")
        void test1() {
            var adapter = buildAdapter(ClientResponse.create(HttpStatus.OK).build());

            assertThatCode(() -> adapter.confirm(UUID.randomUUID()).block())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws on missing payment id")
        void test2() {
            var adapter = buildAdapter(responseWithBody(
                    HttpStatus.NOT_FOUND,
                    errorResponseJson("Payment not found")
            ));

            assertThatExceptionOfType(PaymentIdNotFoundException.class)
                    .isThrownBy(() -> adapter.confirm(UUID.randomUUID()).block())
                    .withMessage("Payment not found");
        }

        @Test
        @DisplayName("throws on service error")
        void test3() {
            var adapter = buildAdapter(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());

            assertThatExceptionOfType(PaymentServiceUnavailableException.class)
                    .isThrownBy(() -> adapter.confirm(UUID.randomUUID()).block())
                    .withMessage("Payment service error");
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("completes successfully")
        void test1() {
            var adapter = buildAdapter(ClientResponse.create(HttpStatus.OK).build());

            assertThatCode(() -> adapter.cancel(UUID.randomUUID()).block())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws on missing payment id")
        void test2() {
            var adapter = buildAdapter(responseWithBody(
                    HttpStatus.NOT_FOUND,
                    errorResponseJson("Payment not found")
            ));

            assertThatExceptionOfType(PaymentIdNotFoundException.class)
                    .isThrownBy(() -> adapter.cancel(UUID.randomUUID()).block())
                    .withMessage("Payment not found");
        }

        @Test
        @DisplayName("throws on service error")
        void test3() {
            var adapter = buildAdapter(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());

            assertThatExceptionOfType(PaymentServiceUnavailableException.class)
                    .isThrownBy(() -> adapter.cancel(UUID.randomUUID()).block())
                    .withMessage("Payment service error");
        }
    }

    private PaymentAdapterImpl buildAdapter(ClientResponse response) {
        ExchangeFunction exchangeFunction = request -> Mono.just(response);
        var apiClient = new ApiClient(WebClient.builder().exchangeFunction(exchangeFunction).build())
                .setBasePath("http://localhost");
        return new PaymentAdapterImpl(new DefaultApi(apiClient), new ObjectMapper());
    }

    private ClientResponse responseWithBody(HttpStatus status, String body) {
        var bufferFactory = new DefaultDataBufferFactory();
        var dataBuffer = bufferFactory.wrap(body.getBytes(StandardCharsets.UTF_8));
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Flux.just(dataBuffer))
                .build();
    }

    private String balanceJson(BigDecimal balance) {
        return "{\"balance\":" + balance + "}";
    }

    private String holdResponseJson(UUID paymentId) {
        return "{\"paymentId\":\"" + paymentId + "\"}";
    }

    private String errorResponseJson(String message) {
        var timestamp = OffsetDateTime.now();
        return "{\"code\":\"PAYMENT_ERROR\",\"message\":\"" + message + "\",\"timestamp\":\"" + timestamp + "\"}";
    }
}
