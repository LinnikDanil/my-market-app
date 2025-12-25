package ru.practicum.market.service;

import reactor.core.publisher.Mono;
import ru.practicum.market.web.dto.CartResponseDto;
import ru.practicum.market.web.dto.ItemResponseDto;
import ru.practicum.market.web.dto.ItemsResponseDto;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.dto.enums.SortMethod;

public interface ItemService {
    Mono<ItemsResponseDto> getItems(String search, SortMethod sort, int pageNumber, int pageSize);

    ItemResponseDto getItem(long id);

    void updateItemsCountInCart(long id, CartAction action);

    CartResponseDto getCart();
}
