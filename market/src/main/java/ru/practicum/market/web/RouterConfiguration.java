package ru.practicum.market.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import ru.practicum.market.web.filter.RouteExceptionFilter;
import ru.practicum.market.web.filter.RouteLoggingFilter;
import ru.practicum.market.web.handler.*;

@Slf4j
@Configuration
public class RouterConfiguration {

    /**
     * Объединяет все маршруты приложения и подключает общие фильтры.
     */
    @Bean
    public RouterFunction<ServerResponse> routes(
            RouterFunction<ServerResponse> itemRoutes,
            RouterFunction<ServerResponse> cartRoutes,
            RouterFunction<ServerResponse> orderRoutes,
            RouterFunction<ServerResponse> adminRoutes,
            RouterFunction<ServerResponse> authRoutes,
            RouteLoggingFilter routeLoggingFilter,
            RouteExceptionFilter routeExceptionFilter
    ) {
        return itemRoutes
                .and(cartRoutes)
                .and(orderRoutes)
                .and(adminRoutes)
                .and(authRoutes)
                .filter(routeLoggingFilter.logging())
                .filter(routeExceptionFilter.errors());
    }

    /**
     * Регистрирует маршруты каталога товаров.
     */
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

    /**
     * Регистрирует маршруты корзины.
     */
    @Bean
    public RouterFunction<ServerResponse> cartRoutes(CartHandler cartHandler) {
        return RouterFunctions.route()
                .path("/cart/items", apiBuilder -> apiBuilder
                        .GET("", cartHandler::getCart)
                        .POST("", cartHandler::updateItemsCountInCart)
                )
                .build();
    }

    /**
     * Регистрирует маршруты заказов.
     */
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

    /**
     * Регистрирует административные маршруты.
     */
    @Bean
    public RouterFunction<ServerResponse> adminRoutes(AdminHandler adminHandler) {
        return RouterFunctions.route()
                .path("/admin", apiBuilder -> apiBuilder
                        .GET("", adminHandler::getAdminPage)
                        .path("/items", itemsBuilder -> itemsBuilder
                                .POST("/upload", adminHandler::uploadItems)
                                .POST("/{id}/image", adminHandler::uploadImage)
                        )
                )
                .build();
    }

    /**
     * Регистрирует маршруты аутентификации.
     */
    @Bean
    public RouterFunction<ServerResponse> authRoutes(AuthHandler authHandler) {
        return RouterFunctions.route()
                .GET("/login", authHandler::login)
                .GET("/registerform", authHandler::registerForm)
                .GET("/access-denied", authHandler::accessDenied)
                .POST("/register", authHandler::register)
                .build();
    }

}
