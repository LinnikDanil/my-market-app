package ru.practicum.payments.web;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebInputException;
import ru.practicum.payments.exception.PaymentBalanceException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DefaultExceptionHandler")
class DefaultExceptionHandlerTest {

    private final DefaultExceptionHandler handler = new DefaultExceptionHandler();

    @Nested
    @DisplayName("exception")
    class ExceptionHandler {

        @Test
        @DisplayName("internal error")
        void test1() {
            var response = handler.exception(new IllegalStateException("boom")).block();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.toString());
            assertThat(response.getBody().getMessage()).isEqualTo("boom");
        }
    }

    @Nested
    @DisplayName("badRequestException")
    class BadRequestException {

        @Test
        @DisplayName("validation error")
        void test1() {
            var response = handler.badRequestException(new ValidationException("invalid")).block();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(HttpStatus.BAD_REQUEST.toString());
        }

        @Test
        @DisplayName("input error")
        void test2() {
            var response = handler.badRequestException(new ServerWebInputException("bad")).block();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("bad");
        }
    }

    @Nested
    @DisplayName("conflictException")
    class ConflictException {

        @Test
        @DisplayName("insufficient funds")
        void test1() {
            var exception = new PaymentBalanceException(
                    BigDecimal.valueOf(100),
                    "There are not enough funds to make the payment."
            );

            var response = handler.conflictException(exception).block();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("INSUFFICIENT_FUNDS");
        }
    }
}
