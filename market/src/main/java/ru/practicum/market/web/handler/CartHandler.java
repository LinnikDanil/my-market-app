package ru.practicum.market.web.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.service.security.CurrentUserService;
import ru.practicum.market.web.bind.QueryBinder;
import ru.practicum.market.web.view.PageRenderHelper;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class CartHandler {

    private final ItemService itemService;
    private final QueryBinder binder;
    private final PageRenderHelper pageRenderHelper;
    private final CurrentUserService userService;

    /**
     * Отображает страницу корзины.
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ServerResponse> getCart(ServerRequest request) {
        return userService.currentUserId(request)
                .flatMap(itemService::getCart)
                .flatMap(cart -> pageRenderHelper.ok(request, "cart", Map.of(
                                "items", cart.items(),
                                "total", cart.total(),
                                "isActive", cart.isActiveButton()
                        ))
                );

    }

    /**
     * Обновляет количество товара в корзине и возвращает актуальную страницу корзины.
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ServerResponse> updateItemsCountInCart(ServerRequest request) {
        var itemId = binder.bindParamId(request);
        var action = binder.bindParamAction(request);

        return userService.currentUserId(request)
                .flatMap(userId -> itemService.updateItemsCountInCart(userId, itemId, action))
                .then(getCart(request));
    }
}
