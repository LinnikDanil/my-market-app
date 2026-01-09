package ru.practicum.payments.web;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import ru.practicum.payments.domain.ErrorResponse;
import ru.practicum.payments.exception.PaymentBalanceException;

@RestControllerAdvice
@Slf4j
public class DefaultExceptionHandler {

    @ExceptionHandler
    public Mono<ResponseEntity<ErrorResponse>> exception(Exception e) {
        log.error("Handled exception of type {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);

        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage()))
        );
    }

    @ExceptionHandler({ServerWebInputException.class, ValidationException.class, WebExchangeBindException.class})
    public Mono<ResponseEntity<ErrorResponse>> badRequestException(Exception e) {
        log.warn("Handled bad request exception of type {}: {}", e.getClass().getSimpleName(), e.getMessage());

        return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(HttpStatus.BAD_REQUEST.toString(), e.getMessage()))
        );
    }

    @ExceptionHandler(PaymentBalanceException.class)
    public Mono<ResponseEntity<ErrorResponse>> conflictException(PaymentBalanceException e) {
        log.warn("Handled conflict exception of type {}: {}", e.getClass().getSimpleName(), e.getMessage());
        return Mono.just(
                ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse("INSUFFICIENT_FUNDS", e.getMessage()))
        );
    }
}
