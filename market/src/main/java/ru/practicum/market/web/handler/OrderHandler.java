package ru.practicum.market.web.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.OrderService;
import ru.practicum.market.service.security.CurrentUserService;
import ru.practicum.market.web.bind.QueryBinder;
import ru.practicum.market.web.view.PageRenderHelper;

import java.net.URI;
import java.util.Map;

/**
 * Обработчик HTTP-сценариев просмотра и создания заказов.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class OrderHandler {

    private final OrderService orderService;
    private final QueryBinder binder;
    private final PageRenderHelper pageRenderHelper;
    private final CurrentUserService userService;

    /**
     * Отображает страницу списка заказов.
     *
     * @param request входящий HTTP-запрос
     * @return серверный ответ с HTML-страницей заказов
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ServerResponse> getOrders(ServerRequest request) {
        log.debug("Rendering orders page");

        var ordersDriver = new ReactiveDataDriverContextVariable(
                userService.currentUserId(request)
                        .flatMapMany(orderService::getOrders),
                10
        );

        return pageRenderHelper.ok(request, "orders", Map.of("orders", ordersDriver));
    }

    /**
     * Отображает страницу конкретного заказа.
     *
     * @param request входящий HTTP-запрос
     * @return серверный ответ с HTML-страницей заказа
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ServerResponse> getOrder(ServerRequest request) {
        var orderId = binder.bindPathVariableId(request);
        boolean newOrder = binder.bindParamNewOrder(request);
        log.debug("Rendering order page for orderId={}, newOrder={}", orderId, newOrder);

        return userService.currentUserId(request)
                .flatMap(userId -> orderService.getOrder(userId, orderId))
                .flatMap(order -> pageRenderHelper.ok(request, "order", Map.of(
                                "order", order,
                                "newOrder", newOrder)
                        )
                );
    }

    /**
     * Создает заказ из корзины и перенаправляет на страницу созданного заказа.
     *
     * @param request входящий HTTP-запрос
     * @return редирект на страницу созданного заказа
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ServerResponse> createOrder(ServerRequest request) {
        log.info("Creating order from cart");
        return userService.currentUserId(request)
                .flatMap(orderService::createOrder)
                .flatMap(id -> {
                    log.info("Order successfully created: orderId={}", id);
                    return ServerResponse.seeOther(URI.create("/orders/%d?newOrder=true".formatted(id))).build();
                });
    }
}
