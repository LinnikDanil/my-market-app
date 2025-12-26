package ru.practicum.market.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import ru.practicum.market.web.controller.ItemHandler;
import ru.practicum.market.web.controller.OrderHandler;

@Configuration
public class RouterConfiguration {

    @Bean
    public RouterFunction<ServerResponse> itemRoutes(ItemHandler itemHandler) {
        return RouterFunctions.route()
                .GET("/", itemHandler::getItems)
                .path("/items", apiBuilder -> apiBuilder
                        .GET("", itemHandler::getItems)
                        .GET("/{id}", itemHandler::getItem)
                        .POST("", itemHandler::updateItemsCountInCartForItems)
                        .POST("/{id}", itemHandler::updateItemsCountInCartForItem)
                )
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> orderRoutes(OrderHandler orderHandler) {
        return RouterFunctions.route()
                .path("/orders", apiBuilder -> apiBuilder
                        .GET("", orderHandler::getOrders)
                        .GET("/{id}", orderHandler::getOrder)
                )
                .POST("/buy", orderHandler::createOrder)
                .build();
    }

//    public RouterFunction<ServerResponse> adminRoutes(AdminHandler adminHandler) {
//        return RouterFunctions.route()
//                .path("/admin/items", apiBuilder -> apiBuilder
//                        .POST("/upload", adminHandler::uploadItems)
//                        .POST("/{id}/image", adminHandler::uploadItems)
//                )
//                .build();
//    }

}
