package ru.practicum.market.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.web.bind.ItemsQueryBinder;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Validated
public class ItemHandler {

    private final ItemService itemService;
    private final ItemsQueryBinder binder;

    public Mono<ServerResponse> getItems(ServerRequest request) {
        var itemsQuery = binder.bind(request);
        return itemService.getItems(
                        itemsQuery.search(),
                        itemsQuery.sort(),
                        itemsQuery.pageNumber(),
                        itemsQuery.pageSize()
                )
                .flatMap(itemsResponseDto ->
                        ServerResponse.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .render("items", Map.of(
                                        "items", itemsResponseDto.items(),
                                        "search", itemsResponseDto.search(),
                                        "sort", itemsResponseDto.sort(),
                                        "paging", itemsResponseDto.paging())
                                )
                );
    }

    public Mono<ServerResponse> getItem(ServerRequest request) {
        var item = itemService.getItem(id);

        model.addAttribute("item", item);

        return "item";
    }

    public Mono<ServerResponse> updateItemsCountInCartForItems(ServerRequest request) {
        itemService.updateItemsCountInCart(id, action);
        return "redirect:/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d"
                .formatted(search, sort, pageNumber, pageSize);
    }

    public Mono<ServerResponse> updateItemsCountInCartForItem(ServerRequest request) {
        itemService.updateItemsCountInCart(id, action);

        return getItem(id, model);
    }

}
