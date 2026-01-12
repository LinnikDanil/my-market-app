package ru.practicum.market.service.cache;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.cache.dto.CartCacheDto;
import ru.practicum.market.service.cache.dto.ItemCacheDto;
import ru.practicum.market.service.cache.dto.ItemsPageCacheDto;

import java.util.List;

public interface ItemCacheService {
    Mono<ItemCacheDto> findItem(long id);

    Mono<ItemsPageCacheDto> getItemsPage(String search, Pageable pageable);

    Mono<CartCacheDto> getItemsByIds(List<Long> itemIds);
}
