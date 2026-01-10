package ru.practicum.market.web.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.service.cache.dto.CartCacheDto;
import ru.practicum.market.service.cache.dto.ItemCacheDto;
import ru.practicum.market.service.cache.dto.ItemsPageCacheDto;
import ru.practicum.market.web.dto.CartResponseDto;
import ru.practicum.market.web.dto.ItemResponseDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class ItemMapper {

    private static final ItemResponseDto MOCK_ITEM = new ItemResponseDto(-1, null, null, null, 0, 0);

    public static ItemResponseDto toItemResponseDto(ItemCacheDto itemCache, Integer itemQuantity) {
        return createItemResponseDto(itemCache, itemQuantity);
    }

    public static List<List<ItemResponseDto>> toItemRows(List<ItemCacheDto> items,
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

    public static CartResponseDto toCart(List<CartItem> cartItems, List<ItemCacheDto> itemsInCart, BigDecimal currentBalance) {
        var itemById = itemsInCart.stream()
                .collect(Collectors.toMap(ItemCacheDto::id, item -> item));
        var quantityByItemId = cartItems.stream()
                .collect(Collectors.toMap(CartItem::getItemId, CartItem::getQuantity));
        var itemsResponseDto = quantityByItemId.entrySet().stream()
                .map(iq -> toItemResponseDto(itemById.get(iq.getKey()), iq.getValue()))
                .toList();
        var totalSum = calculateTotalSum(quantityByItemId, itemById);
        var isActive = currentBalance.compareTo(BigDecimal.valueOf(totalSum)) >= 0;

        return new CartResponseDto(itemsResponseDto, totalSum, isActive);
    }

    public static ItemsPageCacheDto toItemsPage(List<Item> items, Long itemsCount) {
        return new ItemsPageCacheDto(toItemsCacheDto(items), itemsCount);
    }

    public static List<ItemCacheDto> toItemsCacheDto(List<Item> items) {
        return items.stream()
                .map(ItemMapper::toItemCacheDto)
                .toList();
    }

    public static ItemCacheDto toItemCacheDto(Item item) {
        return new ItemCacheDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice()
        );
    }

    public static CartCacheDto toCartCacheDto(List<Item> items) {
        var itemsInCart = items.stream()
                .map(ItemMapper::toItemCacheDto)
                .toList();
        return new CartCacheDto(itemsInCart);

    }

    private static List<ItemResponseDto> itemsToItemResponseDtos(List<ItemCacheDto> items, Map<Long, Integer> itemsQuantity) {
        return items.stream()
                .map(item -> toItemResponseDto(item, itemsQuantity.getOrDefault(item.id(), 0)))
                .toList();
    }

    private static long calculateTotalSum(Map<Long, Integer> quantityByItemId, Map<Long, ItemCacheDto> itemById) {
        return quantityByItemId.entrySet().stream()
                .map(ic -> itemById.get(ic.getKey()).price() * ic.getValue())
                .reduce(0L, Long::sum);
    }

    private static ItemResponseDto createItemResponseDto(ItemCacheDto item, int quantity) {
        return new ItemResponseDto(
                item.id(),
                item.title(),
                item.description(),
                item.imgPath(),
                item.price(),
                quantity
        );
    }
}
