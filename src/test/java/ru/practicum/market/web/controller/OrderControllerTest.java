package ru.practicum.market.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.market.service.OrderService;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.market.web.dto.OrderResponseDto;

import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = OrderController.class)
@DisplayName("OrderController")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Nested
    @DisplayName("getOrders")
    class getOrders {

        @Test
        @DisplayName("ok")
        void test1() throws Exception {
            var orders = List.of(TestDataFactory.createOrderResponseDto(1L,
                    TestDataFactory.createItemResponseDtos(2), 400L));
            doReturn(orders).when(orderService).getOrders();

            mockMvc.perform(get("/orders"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("orders"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("orders", orders));

            verify(orderService, times(1)).getOrders();
        }
    }

    @Nested
    @DisplayName("getOrder")
    class getOrder {

        @Test
        @DisplayName("ok")
        void test1() throws Exception {
            var orderId = 2L;
            var order = new OrderResponseDto(orderId, TestDataFactory.createItemResponseDtos(1), 200L);
            doReturn(order).when(orderService).getOrder(orderId);

            mockMvc.perform(get("/orders/{id}", orderId)
                            .param("newOrder", String.valueOf(true)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("order"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("order", order))
                    .andExpect(model().attribute("newOrder", true));

            verify(orderService, times(1)).getOrder(orderId);
        }
    }

    @Nested
    @DisplayName("createOrder")
    class createOrder {

        @Test
        @DisplayName("ok")
        void test1() throws Exception {
            var orderId = 5L;
            doReturn(orderId).when(orderService).createOrder();

            mockMvc.perform(post("/buy"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/orders/%d?newOrder=true".formatted(orderId)));

            verify(orderService, times(1)).createOrder();
        }
    }
}
