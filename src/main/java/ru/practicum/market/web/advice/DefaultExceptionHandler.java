package ru.practicum.market.web.advice;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.practicum.market.domain.exception.CartItemNotFoundException;
import ru.practicum.market.domain.exception.ItemImageBadRequest;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.domain.exception.ItemUploadException;
import ru.practicum.market.domain.exception.NotFoundExceptionAbstract;
import ru.practicum.market.domain.exception.OrderConflictException;
import ru.practicum.market.domain.exception.OrderNotFoundException;
import ru.practicum.market.web.controller.AdminController;
import ru.practicum.market.web.controller.CartController;
import ru.practicum.market.web.controller.ItemController;
import ru.practicum.market.web.controller.OrderController;

@ControllerAdvice(assignableTypes = {
        CartController.class,
        ItemController.class,
        OrderController.class,
        AdminController.class
})
@Slf4j
public class DefaultExceptionHandler {


    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String exception(Exception e) {
        log.error("Handled exception of type {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);

        return "oops";
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String methodArgumentNotValidException(Exception e) {
        logException(e);
        return "bad-request";
    }

    @ExceptionHandler({ItemImageBadRequest.class, ItemUploadException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String adminBadRequestException(RuntimeException e) {
        logException(e);
        return "admin-error";
    }

    @ExceptionHandler({ItemNotFoundException.class, CartItemNotFoundException.class, OrderNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFoundException(NotFoundExceptionAbstract e, Model model) {
        logException(e);
        model.addAttribute("id", e.getId());
        return "not-found";
    }

    @ExceptionHandler(OrderConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String postDbException(RuntimeException e) {
        logException(e);
        return "conflict";
    }

    private void logException(Exception e) {
        log.error("Handled exception of type {}: {}", e.getClass().getSimpleName(), e.getMessage());
    }
}
