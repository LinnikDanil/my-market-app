package ru.practicum.market.web.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.domain.model.OrderItem;
import ru.practicum.market.web.dto.CartResponseDto;
import ru.practicum.market.web.dto.ItemResponseDto;
import ru.practicum.market.web.dto.ItemShortResponseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class ItemMapper {

    private static final ItemResponseDto MOCK_ITEM = new ItemResponseDto(-1, null, null, null, 0, 0);

    public static ItemResponseDto toItemResponseDto(Item item, Integer itemQuantity) {
        return createItemResponseDto(item, itemQuantity);
    }

    public static List<ItemResponseDto> itemsToItemResponseDtos(List<Item> items, Map<Long, Integer> itemsQuantity) {
        return items.stream()
                .map(item -> toItemResponseDto(item, itemsQuantity.getOrDefault(item.getId(), 0)))
                .toList();
    }

    public static List<List<ItemResponseDto>> toItemRows(List<Item> items,
                                                         Map<Long, Integer> itemsQuantity,
                                                         int rowSize) {
        List<List<ItemResponseDto>> itemRows = new ArrayList<>();
        var itemDtos = itemsToItemResponseDtos(items, itemsQuantity);

        for (int i = 0; i < itemDtos.size(); i += rowSize) {
            List<ItemResponseDto> threeItems = new ArrayList<>(rowSize);
            for (int j = 0; j < rowSize; j++) {
                int index = i + j;

                if (index < itemDtos.size()) {
                    threeItems.add(itemDtos.get(index));
                } else {
                    threeItems.add(MOCK_ITEM);
                }
            }
            itemRows.add(threeItems);
        }

        return itemRows;
    }

    public static CartResponseDto toCart(List<CartItem> cartItems, List<Item> itemsInCart) {
        var itemById = itemsInCart.stream()
                .collect(Collectors.toMap(Item::getId, item -> item));
        var quantityByItemId = cartItems.stream()
                .collect(Collectors.toMap(CartItem::getItemId, CartItem::getQuantity));
        var itemsResponseDto = quantityByItemId.entrySet().stream()
                .map(iq -> toItemResponseDto(itemById.get(iq.getKey()), iq.getValue()))
                .toList();

        return new CartResponseDto(itemsResponseDto, calculateTotalSum(quantityByItemId, itemById));
    }

    private static long calculateTotalSum(Map<Long, Integer> quantityByItemId, Map<Long, Item> itemById) {
        return quantityByItemId.entrySet().stream()
                .map(ic -> itemById.get(ic.getKey()).getPrice() * ic.getValue())
                .reduce(0L, Long::sum);
    }

    private static ItemResponseDto createItemResponseDto(Item item, int quantity) {
        return new ItemResponseDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice(),
                quantity
        );
    }

    public static List<ItemShortResponseDto> toShortResponseDtos(List<Item> items) {
        return items.stream()
                .map(ItemMapper::toShortResponseDto)
                .toList();
    }

    private static ItemShortResponseDto toShortResponseDto(Item item) {
        return new ItemShortResponseDto(item.getId(), item.getTitle());
    }
}
