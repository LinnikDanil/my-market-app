package ru.practicum.market.web.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.OrderService;
import ru.practicum.market.web.bind.QueryBinder;
import ru.practicum.market.web.view.PageRenderHelper;

import java.net.URI;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderHandler {

    private final OrderService orderService;
    private final QueryBinder binder;
    private final PageRenderHelper pageRenderHelper;

    /**
     * Отображает страницу списка заказов.
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ServerResponse> getOrders(ServerRequest request) {

        var ordersDriver = new ReactiveDataDriverContextVariable(
                orderService.getOrders(),
                10
        );

        return pageRenderHelper.ok(request, "orders", Map.of("orders", ordersDriver));
    }

    /**
     * Отображает страницу конкретного заказа.
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ServerResponse> getOrder(ServerRequest request) {
        var id = binder.bindPathVariableId(request);
        boolean newOrder = binder.bindParamNewOrder(request);

        return orderService.getOrder(id)
                .flatMap(order -> pageRenderHelper.ok(request, "order", Map.of(
                        "order", order,
                        "newOrder", newOrder)
                ));
    }

    /**
     * Создает заказ из корзины и перенаправляет на страницу созданного заказа.
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ServerResponse> createOrder(ServerRequest request) {
        return orderService.createOrder()
                .flatMap(id ->
                        ServerResponse.seeOther(URI.create("/orders/%d?newOrder=true".formatted(id))).build()
                );
    }
}
