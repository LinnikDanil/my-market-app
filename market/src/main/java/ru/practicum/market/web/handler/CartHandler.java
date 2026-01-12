package ru.practicum.market.web.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.web.bind.QueryBinder;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class CartHandler {

    private final ItemService itemService;
    private final QueryBinder binder;

    public Mono<ServerResponse> getCart(ServerRequest request) {
        return itemService.getCart()
                .flatMap(cart ->
                        ServerResponse.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .render("cart", Map.of(
                                        "items", cart.items(),
                                        "total", cart.total(),
                                        "isActive", cart.isActiveButton()
                                ))
                );
    }

    public Mono<ServerResponse> updateItemsCountInCart(ServerRequest request) {
        var id = binder.bindParamId(request);
        var action = binder.bindParamAction(request);

        return itemService.updateItemsCountInCart(id, action)
                .then(getCart(request));
    }
}
