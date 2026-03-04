package ru.practicum.market.service;

import reactor.core.publisher.Mono;
import ru.practicum.market.web.dto.CartResponseDto;
import ru.practicum.market.web.dto.ItemResponseDto;
import ru.practicum.market.web.dto.ItemsResponseDto;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.dto.enums.SortMethod;

import java.util.Optional;

/**
 * Сервис пользовательских сценариев каталога и корзины.
 */
public interface ItemService {
    /**
     * Возвращает страницу товаров с фильтрацией, сортировкой и пагинацией.
     *
     * @param userIdOpt
     * @param search     строка поиска по товарам
     * @param sort       способ сортировки
     * @param pageNumber номер страницы (с 1)
     * @param pageSize   размер страницы
     * @return DTO страницы товаров
     */
    Mono<ItemsResponseDto> getItems(Optional<Long> userIdOpt, String search, SortMethod sort, int pageNumber, int pageSize);

    /**
     * Возвращает карточку товара и текущее количество в корзине.
     *
     * @param userIdOpt индентификатор пользователя
     * @param itemId    идентификатор товара
     * @return DTO карточки товара
     */
    Mono<ItemResponseDto> getItem(Optional<Long> userIdOpt, long itemId);

    /**
     * Изменяет количество товара в корзине по указанному действию.
     *
     * @param userId индентификатор пользователя
     * @param itemId идентификатор товара
     * @param action действие над количеством
     * @return сигнал завершения операции
     */
    Mono<Void> updateItemsCountInCart(long userId, long itemId, CartAction action);

    /**
     * Возвращает корзину с учетом текущего баланса пользователя.
     *
     * @param userId индентификатор пользователя
     * @return DTO корзины
     */
    Mono<CartResponseDto> getCart(Long userId);

    /**
     * Возвращает корзину без обращения к платежному сервису.
     *
     * @return DTO корзины
     */
    Mono<CartResponseDto> getCartWithoutPayments(long userId);
}
