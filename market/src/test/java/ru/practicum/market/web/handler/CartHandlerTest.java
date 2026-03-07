package ru.practicum.market.web.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.service.security.CurrentUserService;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.market.web.bind.QueryBinder;
import ru.practicum.market.web.dto.CartResponseDto;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.filter.RouteExceptionFilter;
import ru.practicum.market.web.filter.RouteLoggingFilter;
import ru.practicum.market.web.view.PageRenderHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@WebFluxTest(excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration.class
})
@Import({
        CartHandlerTest.TestRoutes.class,
        CartHandler.class,
        RouteLoggingFilter.class,
        RouteExceptionFilter.class,
        PageRenderHelper.class
})
@DisplayName("CartHandler")
class CartHandlerTest {

    private static final long USER_ID = TestDataFactory.USER_ID;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private QueryBinder binder;

    @MockitoBean
    private CurrentUserService userService;

    @TestConfiguration
    static class TestRoutes {
        @Bean
        WebSessionServerCsrfTokenRepository csrfTokenRepository() {
            return new WebSessionServerCsrfTokenRepository();
        }

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

        when(userService.currentUserId(any())).thenReturn(Mono.just(USER_ID));
        when(itemService.getCart(USER_ID)).thenReturn(Mono.just(cart));

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
        var itemId = 5L;
        var items = TestDataFactory.createItemResponseDtos(3);
        var cart = new CartResponseDto(items, 800L, true);

        when(binder.bindParamId(any())).thenReturn(itemId);
        when(binder.bindParamAction(any())).thenReturn(action);
        when(userService.currentUserId(any())).thenReturn(Mono.just(USER_ID));
        when(itemService.updateItemsCountInCart(USER_ID, itemId, action)).thenReturn(Mono.empty());
        when(itemService.getCart(USER_ID)).thenReturn(Mono.just(cart));

        webTestClient.post()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("title1"));

        verify(itemService, times(1)).updateItemsCountInCart(eq(USER_ID), anyLong(), any());
        verify(itemService, times(1)).getCart(USER_ID);
    }
}
