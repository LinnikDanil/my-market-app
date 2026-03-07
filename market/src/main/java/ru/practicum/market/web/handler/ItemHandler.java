package ru.practicum.market.web.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.service.security.CurrentUserService;
import ru.practicum.market.web.bind.QueryBinder;
import ru.practicum.market.web.dto.ItemsResponseDto;
import ru.practicum.market.web.view.PageRenderHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Обработчик пользовательских HTTP-сценариев каталога товаров.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class ItemHandler {

    private final ItemService itemService;
    private final QueryBinder binder;
    private final PageRenderHelper pageRenderHelper;
    private final CurrentUserService userService;

    /**
     * Отображает страницу каталога товаров.
     *
     * @param request входящий HTTP-запрос
     * @return серверный ответ с HTML-страницей каталога
     */
    @PreAuthorize("permitAll()")
    public Mono<ServerResponse> getItems(ServerRequest request) {
        var itemsQuery = binder.bindItemsQuery(request);
        log.debug("Rendering items page: search='{}', sort={}, pageNumber={}, pageSize={}",
                itemsQuery.search(), itemsQuery.sort(), itemsQuery.pageNumber(), itemsQuery.pageSize());

        return userService.currentUserIdIfAuthenticated(request)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(userIdOpt -> itemService.getItems(
                                userIdOpt,
                                itemsQuery.search(),
                                itemsQuery.sort(),
                                itemsQuery.pageNumber(),
                                itemsQuery.pageSize()
                        )
                )
                .flatMap(itemsResponseDto ->
                        pageRenderHelper.ok(request, "items", buildItemsModel(itemsResponseDto))
                );
    }

    /**
     * Отображает страницу конкретного товара.
     *
     * @param request входящий HTTP-запрос
     * @return серверный ответ с HTML-страницей товара
     */
    @PreAuthorize("permitAll()")
    public Mono<ServerResponse> getItem(ServerRequest request) {
        var id = binder.bindPathVariableId(request);
        log.debug("Rendering item page for itemId={}", id);
        return userService.currentUserIdIfAuthenticated(request)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(userIdOpt -> getItemById(userIdOpt, request, id));
    }

    /**
     * Обновляет количество товара в корзине из списка товаров и делает redirect обратно в каталог.
     *
     * @param request входящий HTTP-запрос
     * @return редирект обратно в каталог
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ServerResponse> updateItemsCountInCartForItems(ServerRequest request) {
        var id = binder.bindParamId(request);
        var action = binder.bindParamAction(request);
        var iq = binder.bindItemsQuery(request);
        log.info("Updating cart from items list: itemId={}, action={}", id, action);
        var redirectUri = UriComponentsBuilder.fromPath("/items")
                .queryParam("search", iq.search())
                .queryParam("sort", iq.sort())
                .queryParam("pageNumber", iq.pageNumber())
                .queryParam("pageSize", iq.pageSize())
                .build(true)
                .toUri();

        return userService.currentUserId(request)
                .flatMap(userId -> itemService.updateItemsCountInCart(userId, id, action))
                .then(ServerResponse.seeOther(redirectUri).build());
    }

    /**
     * Обновляет количество товара в корзине со страницы карточки товара.
     *
     * @param request входящий HTTP-запрос
     * @return серверный ответ с обновлённой карточкой товара
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ServerResponse> updateItemsCountInCartForItem(ServerRequest request) {
        var id = binder.bindPathVariableId(request);
        var action = binder.bindParamAction(request);
        log.info("Updating cart from item page: itemId={}, action={}", id, action);

        return userService.currentUserId(request)
                .flatMap(userId -> itemService.updateItemsCountInCart(userId, id, action)
                        .then(getItemById(Optional.of(userId), request, id))
                );
    }

    /**
     * Рендерит карточку товара по itemId.
     *
     * @param userIdOpt идентификатор пользователя, если пользователь аутентифицирован
     * @param request   входящий HTTP-запрос
     * @param itemId    идентификатор товара
     * @return серверный ответ с HTML-страницей товара
     */
    private Mono<ServerResponse> getItemById(Optional<Long> userIdOpt, ServerRequest request, long itemId) {
        return itemService.getItem(userIdOpt, itemId)
                .flatMap(item ->
                        pageRenderHelper.ok(request, "item", Map.of("item", item))
                );
    }

    /**
     * Собирает модель для шаблона страницы каталога.
     *
     * @param itemsResponseDto DTO ответа страницы каталога
     * @return модель для рендера шаблона
     */
    private Map<String, Object> buildItemsModel(ItemsResponseDto itemsResponseDto) {
        var model = new HashMap<String, Object>();
        model.put("items", itemsResponseDto.items());
        model.put("search", itemsResponseDto.search());
        model.put("sort", itemsResponseDto.sort().name());
        model.put("paging", itemsResponseDto.paging());
        return model;
    }
}
