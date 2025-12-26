package ru.practicum.market.web.mapper;

import lombok.experimental.UtilityClass;
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

    private static OrderResponseDto toOrderResponseDto(Order order, List<OrderItem> orderItems, Map<Long, Item> itemsById) {
        return new OrderResponseDto(
                order.getId(),
                toItemResponseDtos(orderItems, itemsById),
                order.getTotalSum()
        );
    }

    public static List<OrderResponseDto> toOrderResponseDtos(List<Order> o, List<OrderItem> oi, List<Item> i) {
        var itemsById = i.stream().collect(Collectors.toMap(Item::getId, item -> item));
        var orderItemsByOrderId = oi.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));


        return o.stream()
                .map(order -> toOrderResponseDto(order, orderItemsByOrderId.get(order.getId()), itemsById))
                .toList();
    }

//
//    public static Order toOrder(List<CartItem> cartItems) {
//        var totalSum = cartItems.stream()
//                .map(i -> i.getQuantity() * i.getItem().getPrice())
//                .reduce(0L, Long::sum);
//
//        return new Order(totalSum);
//    }
//
//    public static List<OrderItem> toOrderItems(List<CartItem> cartItems, Order order) {
//        return cartItems.stream()
//                .map(ci -> OrderItem.builder()
//                        .order(order)
//                        .item(ci.getItem())
//                        .quantity(ci.getQuantity())
//                        .priceAtOrder(ci.getItem().getPrice())
//                        .build()
//                )
//                .toList();
//    }

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
}
