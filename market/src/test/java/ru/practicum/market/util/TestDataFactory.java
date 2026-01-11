package ru.practicum.market.util;

import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.domain.model.Order;
import ru.practicum.market.web.dto.ItemResponseDto;
import ru.practicum.market.web.dto.OrderResponseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TestDataFactory {

    public static List<Item> createItems(int size) {
        var items = new ArrayList<Item>();
        for (long i = 1; i <= size; i++) {
            items.add(createItem(i));
        }
        return items;
    }

    public static List<Item> createItemsForSave(int size) {
        var items = new ArrayList<Item>();
        for (int i = 1; i <= size; i++) {
            items.add(createItemForSave(i));
        }
        return items;
    }

    public static Item createItem(Long id) {
        var item = new Item(
                "title" + id,
                "description" + id,
                "imgPath" + id,
                100 * id
        );
        item.setId(id);
        return item;
    }

    public static Item createItemForSave(int counter) {
        return new Item(
                "title" + counter,
                "description" + counter,
                "imgPath" + counter,
                100L * counter);
    }

    public static CartItem createCartItem(long itemId, int quantity) {
        var cartItem = new CartItem();
        cartItem.setItemId(itemId);
        cartItem.setQuantity(quantity);
        return cartItem;
    }

    public static Order createOrder(Long id, long totalSum) {
        var order = new Order(totalSum);
        order.setId(id);
        return order;
    }

    public static Order createOrder(long totalSum) {
        return new Order(totalSum);
    }

    public static OrderResponseDto createOrderResponseDto(Long id, List<ItemResponseDto> items, long total) {
        return new OrderResponseDto(id, items, total);
    }


    public static ItemResponseDto createItemResponseDto(Long id, int quantity) {
        return new ItemResponseDto(
                id,
                "title" + id,
                "description" + id,
                "imgPath" + id,
                100 * id,
                quantity
        );
    }

    public static List<ItemResponseDto> createItemResponseDtos(int size) {
        var items = new ArrayList<ItemResponseDto>();
        for (long i = 1; i <= size; i++) {
            items.add(createItemResponseDto(i, ThreadLocalRandom.current().nextInt(1, 11)));
        }
        return items;
    }
}
