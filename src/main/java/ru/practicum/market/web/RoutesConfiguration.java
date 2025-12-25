package ru.practicum.market.web;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import ru.practicum.market.web.controller.ItemHandler;

@Slf4j
public class RoutesConfiguration {

    public RouterFunction<ServerResponse> itemRoutes(ItemHandler itemHandler) {
        return RouterFunctions.route()
                .GET("/", itemHandler::getItems)
                .path("/items", apiBuilder -> apiBuilder
                        .GET("", itemHandler::getItems)
                        .GET("/{id}", itemHandler::getItem)
                        .POST("", itemHandler::updateItemsCountInCartForItems)
                        .POST("{id}", itemHandler::updateItemsCountInCartForItem)
                )
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

    private void logException(Exception e, Level level) {
        log.atLevel(level).log("Handled exception of type {}: {}", e.getClass().getSimpleName(), e.getMessage());
    }

}
