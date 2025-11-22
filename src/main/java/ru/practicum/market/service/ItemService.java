package ru.practicum.market.service;

import ru.practicum.market.web.dto.ItemsResponseDto;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.dto.enums.SortMethod;

public interface ItemService {
    ItemsResponseDto getItems(String search, SortMethod sort, int pageNumber, int pageSize);

    void updateItemsCountInCart(long id, CartAction action);
}
