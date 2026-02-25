package ru.practicum.market.web.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.web.bind.QueryBinder;
import ru.practicum.market.web.dto.ItemsResponseDto;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class ItemHandler {

    private final ItemService itemService;
    private final QueryBinder binder;

    /**
     * Отображает страницу каталога товаров.
     */
    public Mono<ServerResponse> getItems(ServerRequest request) {
        var itemsQuery = binder.bindItemsQuery(request);
        return itemService.getItems(
                        itemsQuery.search(),
                        itemsQuery.sort(),
                        itemsQuery.pageNumber(),
                        itemsQuery.pageSize()
                )
                .flatMap(itemsResponseDto ->
                        ServerResponse.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .render("items", buildItemsModel(itemsResponseDto))
                );
    }

    /**
     * Отображает страницу конкретного товара.
     */
    public Mono<ServerResponse> getItem(ServerRequest request) {
        var id = binder.bindPathVariableId(request);
        return getItemById(id);
    }

    /**
     * Обновляет количество товара в корзине из списка товаров и делает redirect обратно в каталог.
     */
    public Mono<ServerResponse> updateItemsCountInCartForItems(ServerRequest request) {
        var id = binder.bindParamId(request);
        var action = binder.bindParamAction(request);
        var iq = binder.bindItemsQuery(request);
        var redirectUri = UriComponentsBuilder.fromPath("/items")
                .queryParam("search", iq.search())
                .queryParam("sort", iq.sort())
                .queryParam("pageNumber", iq.pageNumber())
                .queryParam("pageSize", iq.pageSize())
                .build(true)
                .toUri();

        return itemService.updateItemsCountInCart(id, action)
                .then(ServerResponse.seeOther(redirectUri).build());
    }

    /**
     * Обновляет количество товара в корзине со страницы карточки товара.
     */
    public Mono<ServerResponse> updateItemsCountInCartForItem(ServerRequest request) {
        var id = binder.bindPathVariableId(request);
        var action = binder.bindParamAction(request);

        return itemService.updateItemsCountInCart(id, action)
                .then(getItemById(id));
    }

    /**
     * Рендерит карточку товара по id.
     */
    private Mono<ServerResponse> getItemById(long id) {
        return itemService.getItem(id)
                .flatMap(item ->
                        ServerResponse.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .render("item", Map.of("item", item)));
    }

    /**
     * Собирает модель для шаблона страницы каталога.
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
