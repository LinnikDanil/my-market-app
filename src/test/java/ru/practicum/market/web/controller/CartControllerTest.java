package ru.practicum.market.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.market.web.dto.CartResponseDto;
import ru.practicum.market.web.dto.enums.CartAction;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = CartController.class)
@DisplayName("CartController")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @Nested
    @DisplayName("getCart")
    class getCart {

        @Test
        @DisplayName("ok")
        void test1() throws Exception {
            var items = TestDataFactory.createItemResponseDtos(2);
            var cart = new CartResponseDto(items, 500L);
            when(itemService.getCart()).thenReturn(cart);

            mockMvc.perform(get("/cart/items"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("cart"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("items", items))
                    .andExpect(model().attribute("total", cart.total()));
        }
    }

    @Nested
    @DisplayName("updateItemsCountInCart")
    class updateItemsCountInCart {

        @Test
        @DisplayName("ok")
        void test1() throws Exception {
            var action = CartAction.PLUS;
            var id = 5L;
            var items = TestDataFactory.createItemResponseDtos(3);
            var cart = new CartResponseDto(items, 800L);
            doNothing().when(itemService).updateItemsCountInCart(id, action);
            when(itemService.getCart()).thenReturn(cart);

            mockMvc.perform(post("/cart/items")
                            .param("id", String.valueOf(id))
                            .param("action", action.name()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("cart"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("items", items))
                    .andExpect(model().attribute("total", cart.total()));

            verify(itemService, times(1)).updateItemsCountInCart(id, action);
            verify(itemService, times(1)).getCart();
        }

        @Test
        @DisplayName("another action")
        void test2() throws Exception {
            var action = CartAction.DELETE;
            var id = 7L;
            var cart = TestDataFactory.createCartResponseDto(TestDataFactory.createItemResponseDtos(1), 200L);
            doNothing().when(itemService).updateItemsCountInCart(anyLong(), eq(action));
            when(itemService.getCart()).thenReturn(cart);

            mockMvc.perform(post("/cart/items")
                            .param("id", String.valueOf(id))
                            .param("action", action.name()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("cart"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("items", cart.items()))
                    .andExpect(model().attribute("total", cart.total()));

            verify(itemService, times(1)).updateItemsCountInCart(id, action);
        }
    }
}
