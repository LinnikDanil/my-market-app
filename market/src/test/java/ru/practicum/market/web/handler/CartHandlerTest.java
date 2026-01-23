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
import reactor.core.publisher.Mono;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.market.web.bind.QueryBinder;
import ru.practicum.market.web.dto.CartResponseDto;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.filter.RouteExceptionFilter;
import ru.practicum.market.web.filter.RouteLoggingFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest
@Import({CartHandlerTest.TestRoutes.class, CartHandler.class, RouteLoggingFilter.class, RouteExceptionFilter.class})
@DisplayName("CartHandler")
class CartHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private QueryBinder binder;

    @TestConfiguration
    static class TestRoutes {
        @Bean
        RouterFunction<ServerResponse> routes(
                CartHandler cartHandler,
                RouteLoggingFilter logging,
                RouteExceptionFilter errors
        ) {
            return RouterFunctions.route()
                    .path("/cart/items", builder -> builder
                            .GET("", cartHandler::getCart)
                            .POST("", cartHandler::updateItemsCountInCart)
                    )
                    .build()
                    .filter(logging.logging())
                    .filter(errors.errors());
        }
    }

    @Test
    @DisplayName("getCart")
    void test1() {
        var items = TestDataFactory.createItemResponseDtos(2);
        var cart = new CartResponseDto(items, 500L, true);
        when(itemService.getCart()).thenReturn(Mono.just(cart));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("title1"));
    }

    @Test
    @DisplayName("updateItemsCountInCart")
    void test2() {
        var action = CartAction.PLUS;
        var id = 5L;
        var items = TestDataFactory.createItemResponseDtos(3);
        var cart = new CartResponseDto(items, 800L, true);
        when(binder.bindParamId(any())).thenReturn(id);
        when(binder.bindParamAction(any())).thenReturn(action);
        when(itemService.updateItemsCountInCart(id, action)).thenReturn(Mono.empty());
        when(itemService.getCart()).thenReturn(Mono.just(cart));

        webTestClient.post()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("title1"));

        verify(itemService, times(1)).updateItemsCountInCart(anyLong(), any());
        verify(itemService, times(1)).getCart();
    }
}
