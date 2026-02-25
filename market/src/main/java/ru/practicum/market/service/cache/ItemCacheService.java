package ru.practicum.market.service.cache;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.cache.dto.CartCacheDto;
import ru.practicum.market.service.cache.dto.ItemCacheDto;
import ru.practicum.market.service.cache.dto.ItemsPageCacheDto;

import java.util.List;

/**
 * Сервис чтения данных каталога из кэша и репозитория.
 */
public interface ItemCacheService {
    /**
     * Возвращает карточку товара для кэша.
     *
     * @param id идентификатор товара
     * @return DTO товара для кэша
     */
    Mono<ItemCacheDto> findItem(long id);

    /**
     * Возвращает страницу товаров для указанного фильтра и пагинации.
     *
     * @param search строка поиска
     * @param pageable параметры пагинации и сортировки
     * @return DTO страницы для кэша
     */
    Mono<ItemsPageCacheDto> getItemsPage(String search, Pageable pageable);

    /**
     * Возвращает данные товаров для корзины по списку id.
     *
     * @param itemIds идентификаторы товаров
     * @return DTO данных корзины для кэша
     */
    Mono<CartCacheDto> getItemsByIds(List<Long> itemIds);
}
