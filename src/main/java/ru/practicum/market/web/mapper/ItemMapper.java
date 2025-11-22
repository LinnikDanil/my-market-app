package ru.practicum.market.web.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.web.dto.CartResponseDto;
import ru.practicum.market.web.dto.ItemResponseDto;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ItemMapper {

    private static final ItemResponseDto MOCK_ITEM = new ItemResponseDto(-1, null, null, null, 0, 0);

    public static ItemResponseDto toItemResponseDto(Item item) {
        return new ItemResponseDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice(),
                item.getCount()
        );
    }

    public static List<List<ItemResponseDto>> toItemRows(List<Item> items, int rowSize) {
        List<List<ItemResponseDto>> itemRows = new ArrayList<>();
        var itemDtos = toItemResponseDtos(items);

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

    public static CartResponseDto toCart(List<Item> itemsInCart) {
        return new CartResponseDto(toItemResponseDtos(itemsInCart), calculateTotalSum(itemsInCart));
    }

    private static long calculateTotalSum(List<Item> itemsInCart) {
        return itemsInCart.stream()
                .map(Item::getPrice)
                .reduce(0L, Long::sum);
    }

    private static List<ItemResponseDto> toItemResponseDtos(List<Item> items) {
        return items.stream()
                .map(ItemMapper::toItemResponseDto)
                .toList();
    }
}
