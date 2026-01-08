package ru.practicum.market.web.dto;

public record ItemResponseDto(
        long id, // -1 если товар - заглушка
        String title,
        String description,
        String imgPath, // путь к файлу, например /images/ball.jpg
        long price, // цена товара
        int count // число товаров в корзине
) {
}
