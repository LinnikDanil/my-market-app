package ru.practicum.market.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.market.domain.exception.CartItemNotFoundException;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.market.web.advice.DefaultExceptionHandler;
import ru.practicum.market.web.dto.ItemsResponseDto;
import ru.practicum.market.web.dto.Paging;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.dto.enums.SortMethod;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = ItemController.class)
@DisplayName("ItemController")
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoSpyBean
    private DefaultExceptionHandler exceptionHandler;

    @Nested
    @DisplayName("getItems")
    class getItems {

        @Test
        @DisplayName("ok")
        void test1() throws Exception {
            String search = "search";
            var sort = SortMethod.NO;
            var pageNumber = 1;
            var pageSize = 5;

            var items = TestDataFactory.createItemResponseDtos(3);
            var paging = new Paging(pageSize, pageNumber, false, false);
            var itemsResponseDto = new ItemsResponseDto(List.of(items), search, sort, paging);

            when(itemService.getItems(search, sort, pageNumber, pageSize)).thenReturn(itemsResponseDto);

            mockMvc.perform(get("/items")
                            .param("search", search)
                            .param("sort", sort.name())
                            .param("pageNumber", String.valueOf(pageNumber))
                            .param("pageSize", String.valueOf(pageSize)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("items"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("items", List.of(items)))
                    .andExpect(model().attribute("search", search))
                    .andExpect(model().attribute("sort", sort))
                    .andExpect(model().attribute("paging", paging));
        }

        @Test
        @DisplayName("empty result")
        void test2() throws Exception {
            String search = "search";
            var sort = SortMethod.NO;
            var pageNumber = 1;
            var pageSize = 5;

            var paging = new Paging(pageSize, pageNumber, false, false);
            var itemsResponseDto = new ItemsResponseDto(Collections.emptyList(), search, sort, paging);

            when(itemService.getItems(search, sort, pageNumber, pageSize)).thenReturn(itemsResponseDto);

            mockMvc.perform(get("/items")
                            .param("search", search)
                            .param("sort", sort.name())
                            .param("pageNumber", String.valueOf(pageNumber))
                            .param("pageSize", String.valueOf(pageSize)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("items"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("items", Collections.emptyList()))
                    .andExpect(model().attribute("search", search))
                    .andExpect(model().attribute("sort", sort))
                    .andExpect(model().attribute("paging", paging));
        }
    }

    @Nested
    @DisplayName("getItem")
    class getItem {

        @Test
        @DisplayName("ok")
        void test1() throws Exception {
            var itemId = 1L;
            var quantity = 2;
            var itemResponseDto = TestDataFactory.createItemResponseDto(1L, quantity);

            when(itemService.getItem(itemId)).thenReturn(itemResponseDto);

            mockMvc.perform(get("/items/{itemId}", itemId))
                    .andExpect(status().isOk())
                    .andExpect(view().name("item"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("item", itemResponseDto));
        }

        @Test
        @DisplayName("not found")
        void test2() throws Exception {
            var itemId = 1L;

            when(itemService.getItem(itemId))
                    .thenThrow(new ItemNotFoundException(itemId, "Item with id = %d not found.".formatted(itemId)));

            mockMvc.perform(get("/items/{itemId}", itemId))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name("not-found"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("id", itemId));
        }
    }

    @Nested
    @DisplayName("updateItemsCountInCartForItems")
    class updateItemsCountInCartForItems {

        @Test
        @DisplayName("ok")
        void test1() throws Exception {
            var itemId = 1L;
            var action = CartAction.PLUS;
            var search = "search";
            var sort = SortMethod.NO;
            var pageNumber = 1;
            var pageSize = 5;

            mockMvc.perform(post("/items")
                            .param("id", String.valueOf(itemId))
                            .param("search", search)
                            .param("sort", sort.name())
                            .param("pageNumber", String.valueOf(pageNumber))
                            .param("pageSize", String.valueOf(pageSize))
                            .param("action", action.name()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(
                            "/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d"
                                    .formatted(search, sort, pageNumber, pageSize))
                    );
        }

        @Test
        @DisplayName("not found cartItem")
        void test2() throws Exception {
            var itemId = 1L;
            var action = CartAction.PLUS;
            var search = "search";
            var sort = SortMethod.NO;
            var pageNumber = 1;
            var pageSize = 5;

            doThrow(new CartItemNotFoundException(itemId, "Cart item with id = %d not found.".formatted(itemId)))
                    .when(itemService).updateItemsCountInCart(itemId, action);

            mockMvc.perform(post("/items")
                            .param("id", String.valueOf(itemId))
                            .param("search", search)
                            .param("sort", sort.name())
                            .param("pageNumber", String.valueOf(pageNumber))
                            .param("pageSize", String.valueOf(pageSize))
                            .param("action", action.name()))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name("not-found"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("id", itemId));
        }
    }

    @Nested
    @DisplayName("updateItemsCountInCartForItem")
    class updateItemsCountInCartForItem {

        @Test
        @DisplayName("ok")
        void test1() throws Exception {
            var itemId = 1L;
            var quantity = 2;
            var itemResponseDto = TestDataFactory.createItemResponseDto(1L, quantity);
            var action = CartAction.PLUS;

            when(itemService.getItem(itemId)).thenReturn(itemResponseDto);

            mockMvc.perform(post("/items/{itemId}", itemId)
                            .param("action", action.name()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("item"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("item", itemResponseDto));
        }

        @Test
        @DisplayName("not found cartItem")
        void test2() throws Exception {
            var itemId = 1L;
            var action = CartAction.PLUS;

            doThrow(new CartItemNotFoundException(itemId, "Cart item with id = %d not found.".formatted(itemId)))
                    .when(itemService).updateItemsCountInCart(itemId, action);

            mockMvc.perform(post("/items/{itemId}", itemId)
                            .param("action", action.name()))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name("not-found"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("id", itemId));
        }
    }
}
