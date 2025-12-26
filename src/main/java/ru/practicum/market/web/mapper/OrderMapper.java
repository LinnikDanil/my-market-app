package ru.practicum.market.web.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.domain.model.Order;
import ru.practicum.market.domain.model.OrderItem;
import ru.practicum.market.web.dto.ItemResponseDto;
import ru.practicum.market.web.dto.OrderResponseDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class OrderMapper {

    public static List<OrderResponseDto> toOrderResponseDtos(
            List<Order> orders,
            List<OrderItem> orderItems,
            List<Item> items
    ) {
        var itemsById = groupItemsById(items);
        var orderItemsByOrderId = orderItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

        return orders.stream()
                .map(order -> createOrderResponseDto(order, orderItemsByOrderId.get(order.getId()), itemsById))
                .toList();
    }

    public static OrderResponseDto toOrderResponseDto(Order order, List<OrderItem> orderItems, List<Item> items) {
        var itemsById = groupItemsById(items);

        return createOrderResponseDto(order, orderItems, itemsById);
    }

    public static Order toOrder(List<CartItem> cartItems, List<Item> items) {
        var itemsById = groupItemsById(items);

        var totalSum = cartItems.stream()
                .map(i -> i.getQuantity() * itemsById.get(i.getItemId()).getPrice())
                .reduce(0L, Long::sum);

        return new Order(totalSum);
    }

    public static List<OrderItem> toOrderItems(List<CartItem> cartItems, List<Item> items, long orderId) {
        var itemsById = groupItemsById(items);

        return cartItems.stream()
                .map(ci ->
                        new OrderItem(
                                orderId,
                                ci.getItemId(),
                                ci.getQuantity(),
                                itemsById.get(ci.getItemId()).getPrice()
                        )
                )
                .toList();
    }

    private static OrderResponseDto createOrderResponseDto(
            Order order,
            List<OrderItem> orderItems,
            Map<Long, Item> itemsById
    ) {
        return new OrderResponseDto(
                order.getId(),
                toItemResponseDtos(orderItems, itemsById),
                order.getTotalSum()
        );
    }

    private static List<ItemResponseDto> toItemResponseDtos(
            List<OrderItem> orderItems,
            Map<Long, Item> itemsById
    ) {
        return orderItems.stream()
                .map(oi -> {
                    var item = itemsById.get(oi.getItemId());
                    return new ItemResponseDto(
                            item.getId(),
                            item.getTitle(),
                            item.getDescription(),
                            item.getImgPath(),
                            oi.getPriceAtOrder(),
                            oi.getQuantity()
                    );
                })
                .toList();
    }

    private static Map<Long, Item> groupItemsById(List<Item> items) {
        return items.stream().collect(Collectors.toMap(Item::getId, item -> item));
    }
}
