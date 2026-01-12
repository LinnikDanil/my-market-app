package ru.practicum.market.web.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.CartItemNotFoundException;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.market.web.bind.QueryBinder;
import ru.practicum.market.web.bind.model.ItemsQuery;
import ru.practicum.market.web.dto.ItemsResponseDto;
import ru.practicum.market.web.dto.Paging;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.dto.enums.SortMethod;
import ru.practicum.market.web.filter.RouteExceptionFilter;
import ru.practicum.market.web.filter.RouteLoggingFilter;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest
@Import({ItemHandlerTest.TestRoutes.class, ItemHandler.class, RouteLoggingFilter.class, RouteExceptionFilter.class})
@DisplayName("ItemHandler")
class ItemHandlerTest {

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
                ItemHandler itemHandler,
                RouteLoggingFilter logging,
                RouteExceptionFilter errors
        ) {
            return RouterFunctions.route()
                    .GET("/", itemHandler::getItems)
                    .path("/items", apiBuilder -> apiBuilder
                            .GET("", itemHandler::getItems)
                            .GET("/{id}", itemHandler::getItem)
                            .POST("", itemHandler::updateItemsCountInCartForItems)
                            .POST("/{id}", itemHandler::updateItemsCountInCartForItem)
                    )
                    .build()
                    .filter(logging.logging())
                    .filter(errors.errors());
        }
    }

    @Nested
    @DisplayName("getItems")
    class getItems {

        @Test
        @DisplayName("ok")
        void test1() {
            String search = "search";
            var sort = SortMethod.NO;
            var pageNumber = 1;
            var pageSize = 5;

            var items = TestDataFactory.createItemResponseDtos(3);
            var paging = new Paging(pageSize, pageNumber, false, false);
            var itemsResponseDto = new ItemsResponseDto(List.of(items), search, sort, paging);

            when(binder.bindItemsQuery(any(ServerRequest.class)))
                    .thenReturn(new ItemsQuery(search, sort, pageNumber, pageSize));
            when(itemService.getItems(search, sort, pageNumber, pageSize))
                    .thenReturn(Mono.just(itemsResponseDto));

            webTestClient.get()
                    .uri("/items")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.TEXT_HTML)
                    .expectBody(String.class)
                    .value(html -> {
                        assert html.contains("title1");
                    });
        }

        @Test
        @DisplayName("empty result")
        void test2() {
            String search = "search";
            var sort = SortMethod.NO;
            var pageNumber = 1;
            var pageSize = 5;

            var paging = new Paging(pageSize, pageNumber, false, false);
            var itemsResponseDto = new ItemsResponseDto(Collections.emptyList(), search, sort, paging);

            when(binder.bindItemsQuery(any(ServerRequest.class)))
                    .thenReturn(new ItemsQuery(search, sort, pageNumber, pageSize));
            when(itemService.getItems(search, sort, pageNumber, pageSize))
                    .thenReturn(Mono.just(itemsResponseDto));

            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/items")
                            .queryParam("search", search)
                            .queryParam("sort", sort.name())
                            .queryParam("pageNumber", pageNumber)
                            .queryParam("pageSize", pageSize)
                            .build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.TEXT_HTML);
        }
    }

    @Nested
    @DisplayName("getItem")
    class getItem {

        @Test
        @DisplayName("ok")
        void test1() {
            var itemId = 1L;
            var quantity = 2;
            var itemResponseDto = TestDataFactory.createItemResponseDto(1L, quantity);

            when(binder.bindPathVariableId(any(ServerRequest.class)))
                    .thenReturn(itemId);
            when(itemService.getItem(itemId))
                    .thenReturn(Mono.just(itemResponseDto));

            webTestClient.get()
                    .uri("/items/{itemId}", itemId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.TEXT_HTML)
                    .expectBody(String.class)
                    .value(html -> {
                        assert html.contains("title" + itemId);
                    });
        }

        @Test
        @DisplayName("not found")
        void test2() {
            var itemId = 1L;

            when(binder.bindPathVariableId(any(ServerRequest.class)))
                    .thenReturn(itemId);
            when(itemService.getItem(itemId))
                    .thenReturn(Mono.error(new ItemNotFoundException(
                            itemId,
                            "Item with id = %d not found.".formatted(itemId)
                    )));

            webTestClient.get()
                    .uri("/items/{itemId}", itemId)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectHeader().contentType(MediaType.TEXT_HTML)
                    .expectBody(String.class)
                    .value(html -> {
                        assert html.contains(String.valueOf(itemId));
                    });
        }
    }

    @Nested
    @DisplayName("updateItemsCountInCartForItems")
    class updateItemsCountInCartForItems {

        @Test
        @DisplayName("ok")
        void test1() {
            var itemId = 1L;
            var action = CartAction.PLUS;
            var search = "search";
            var sort = SortMethod.NO;
            var pageNumber = 1;
            var pageSize = 5;

            when(binder.bindParamId(any(ServerRequest.class)))
                    .thenReturn(itemId);
            when(binder.bindParamAction(any(ServerRequest.class)))
                    .thenReturn(action);
            when(binder.bindItemsQuery(any(ServerRequest.class)))
                    .thenReturn(new ItemsQuery(search, sort, pageNumber, pageSize));
            when(itemService.updateItemsCountInCart(itemId, action))
                    .thenReturn(Mono.empty());

            webTestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/items")
                            .queryParam("id", itemId)
                            .queryParam("search", search)
                            .queryParam("sort", sort.name())
                            .queryParam("pageNumber", pageNumber)
                            .queryParam("pageSize", pageSize)
                            .queryParam("action", action.name())
                            .build())
                    .exchange()
                    .expectStatus().is3xxRedirection()
                    .expectHeader().location(
                            "/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d"
                                    .formatted(search, sort, pageNumber, pageSize)
                    );
        }

        @Test
        @DisplayName("not found cartItem")
        void test2() {
            var itemId = 1L;
            var action = CartAction.PLUS;
            var search = "search";
            var sort = SortMethod.NO;
            var pageNumber = 1;
            var pageSize = 5;

            when(itemService.updateItemsCountInCart(itemId, action))
                    .thenReturn(Mono.error(new CartItemNotFoundException(
                            itemId,
                            "Cart item with id = %d not found.".formatted(itemId)
                    )));

            when(binder.bindParamId(any(ServerRequest.class)))
                    .thenReturn(itemId);
            when(binder.bindParamAction(any(ServerRequest.class)))
                    .thenReturn(action);
            when(binder.bindItemsQuery(any(ServerRequest.class)))
                    .thenReturn(new ItemsQuery(search, sort, pageNumber, pageSize));

            webTestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/items")
                            .queryParam("id", itemId)
                            .queryParam("search", search)
                            .queryParam("sort", sort.name())
                            .queryParam("pageNumber", pageNumber)
                            .queryParam("pageSize", pageSize)
                            .queryParam("action", action.name())
                            .build())
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectHeader().contentType(MediaType.TEXT_HTML)
                    .expectBody(String.class)
                    .value(html -> {
                        assert html.contains(String.valueOf(itemId));
                    });
        }
    }

    @Nested
    @DisplayName("updateItemsCountInCartForItem")
    class updateItemsCountInCartForItem {

        @Test
        @DisplayName("ok")
        void test1() {
            var itemId = 1L;
            var quantity = 2;
            var itemResponseDto = TestDataFactory.createItemResponseDto(1L, quantity);
            var action = CartAction.PLUS;

            when(binder.bindPathVariableId(any(ServerRequest.class)))
                    .thenReturn(itemId);
            when(binder.bindParamAction(any(ServerRequest.class)))
                    .thenReturn(action);
            when(itemService.updateItemsCountInCart(itemId, action))
                    .thenReturn(Mono.empty());
            when(itemService.getItem(itemId))
                    .thenReturn(Mono.just(itemResponseDto));

            webTestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/items/{itemId}")
                            .queryParam("action", action.name())
                            .build(itemId))
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.TEXT_HTML)
                    .expectBody(String.class)
                    .value(html -> {
                        assert html.contains("title" + itemId);
                    });
        }

        @Test
        @DisplayName("not found cartItem")
        void test2() {
            var itemId = 1L;
            var action = CartAction.PLUS;

            when(itemService.updateItemsCountInCart(itemId, action))
                    .thenReturn(Mono.error(new CartItemNotFoundException(
                            itemId,
                            "Cart item with id = %d not found.".formatted(itemId)
                    )));

            when(binder.bindPathVariableId(any(ServerRequest.class)))
                    .thenReturn(itemId);
            when(binder.bindParamAction(any(ServerRequest.class)))
                    .thenReturn(action);

            webTestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/items/{itemId}")
                            .queryParam("action", action.name())
                            .build(itemId))
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectHeader().contentType(MediaType.TEXT_HTML)
                    .expectBody(String.class)
                    .value(html -> {
                        assert html.contains(String.valueOf(itemId));
                    });
        }
    }
}
