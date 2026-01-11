package ru.practicum.market.web.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.service.OrderService;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.market.web.bind.QueryBinder;
import ru.practicum.market.web.dto.OrderResponseDto;
import ru.practicum.market.web.filter.RouteExceptionFilter;
import ru.practicum.market.web.filter.RouteLoggingFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest
@Import({OrderHandlerTest.TestRoutes.class, OrderHandler.class, RouteLoggingFilter.class, RouteExceptionFilter.class})
@DisplayName("OrderHandler")
class OrderHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private QueryBinder binder;

    @TestConfiguration
    static class TestRoutes {
        @Bean
        RouterFunction<ServerResponse> routes(
                OrderHandler orderHandler,
                RouteLoggingFilter logging,
                RouteExceptionFilter errors
        ) {
            return RouterFunctions.route()
                    .path("/orders", builder -> builder
                            .GET("", orderHandler::getOrders)
                            .GET("/{id}", orderHandler::getOrder)
                    )
                    .POST("/buy", orderHandler::createOrder)
                    .build()
                    .filter(logging.logging())
                    .filter(errors.errors());
        }
    }

    @Test
    @DisplayName("getOrders")
    void test1() {
        var orders = List.of(TestDataFactory.createOrderResponseDto(1L,
                TestDataFactory.createItemResponseDtos(2), 400L));
        when(orderService.getOrders()).thenReturn(Flux.fromIterable(orders));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("title1"));

        verify(orderService, times(1)).getOrders();
    }

    @Test
    @DisplayName("getOrder")
    void test2() {
        var orderId = 2L;
        var order = new OrderResponseDto(orderId, TestDataFactory.createItemResponseDtos(1), 200L);
        when(binder.bindPathVariableId(any())).thenReturn(orderId);
        when(binder.bindParamNewOrder(any())).thenReturn(true);
        when(orderService.getOrder(orderId)).thenReturn(Mono.just(order));

        webTestClient.get()
                .uri("/orders/{id}?newOrder=true", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("title1"));

        verify(orderService, times(1)).getOrder(orderId);
    }

    @Test
    @DisplayName("createOrder")
    void test3() {
        var orderId = 5L;
        when(orderService.createOrder()).thenReturn(Mono.just(orderId));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/orders/5?newOrder=true");

        verify(orderService, times(1)).createOrder();
    }
}
