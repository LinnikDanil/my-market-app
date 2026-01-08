package ru.practicum.market.web.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.OrderService;
import ru.practicum.market.web.bind.QueryBinder;

import java.net.URI;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderHandler {

    private final OrderService orderService;
    private final QueryBinder binder;

    public Mono<ServerResponse> getOrders(ServerRequest request) {

        var ordersDriver = new ReactiveDataDriverContextVariable(
                orderService.getOrders(),
                10
        );

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .render("orders", Map.of("orders", ordersDriver));
    }

    public Mono<ServerResponse> getOrder(ServerRequest request) {
        var id = binder.bindPathVariableId(request);
        boolean newOrder = binder.bindParamNewOrder(request);

        return orderService.getOrder(id)
                .flatMap(order ->
                        ServerResponse.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .render("order", Map.of(
                                        "order", order,
                                        "newOrder", newOrder)
                                )
                );
    }

    public Mono<ServerResponse> createOrder(ServerRequest request) {
        return orderService.createOrder()
                .flatMap(id ->
                        ServerResponse.seeOther(URI.create("/orders/%d?newOrder=true".formatted(id))).build()
                );
    }
}
