package ru.practicum.market.web.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.domain.model.Order;
import ru.practicum.market.domain.model.OrderItem;
import ru.practicum.market.web.dto.OrderResponseDto;

import java.util.List;

@UtilityClass
public class OrderMapper {

    public static OrderResponseDto toOrderResponseDto(Order order) {
        return new OrderResponseDto(
                order.getId(),
                ItemMapper.orderItemsToItemResponseDtos(order.getOrderItems()),
                order.getTotalSum()
        );
    }

    public static List<OrderResponseDto> toOrderResponseDtos(List<Order> orders) {
        return orders.stream()
                .map(OrderMapper::toOrderResponseDto)
                .toList();
    }

    public static Order toOrder(List<CartItem> cartItems) {
        var totalSum = cartItems.stream()
                .map(i -> i.getQuantity() * i.getItem().getPrice())
                .reduce(0L, Long::sum);

        return new Order(totalSum);
    }

    public static List<OrderItem> toOrderItems(List<CartItem> cartItems, Order order) {
        return cartItems.stream()
                .map(ci -> OrderItem.builder()
                        .order(order)
                        .item(ci.getItem())
                        .quantity(ci.getQuantity())
                        .priceAtOrder(ci.getItem().getPrice())
                        .build()
                )
                .toList();
    }
}
