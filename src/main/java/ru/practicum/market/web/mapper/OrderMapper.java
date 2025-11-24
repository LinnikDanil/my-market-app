package ru.practicum.market.web.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.domain.model.Order;
import ru.practicum.market.web.dto.OrderResponseDto;

import java.util.List;

@UtilityClass
public class OrderMapper {

    public static OrderResponseDto toOrderResponseDto(Order order) {
        return new OrderResponseDto(
                order.getId(),
                ItemMapper.toItemResponseDtos(order.getItems()),
                order.getTotalSum()
        );
    }

    public static List<OrderResponseDto> toOrderResponseDtos(List<Order> orders) {
        return orders.stream()
                .map(OrderMapper::toOrderResponseDto)
                .toList();
    }

    public static Order toOrder(List<Item> items) {
        var totalSum = items.stream()
                .map(i -> i.getCount() * i.getPrice())
                .reduce(0L, Long::sum);

        return Order.builder()
                .items(items)
                .totalSum(totalSum)
                .build();
    }
}
