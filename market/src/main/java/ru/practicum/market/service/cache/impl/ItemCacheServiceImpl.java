package ru.practicum.market.service.cache.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.service.cache.ItemCacheService;
import ru.practicum.market.service.cache.dto.CartCacheDto;
import ru.practicum.market.service.cache.dto.ItemCacheDto;
import ru.practicum.market.service.cache.dto.ItemsPageCacheDto;
import ru.practicum.market.service.cache.util.KeyGenerator;
import ru.practicum.market.web.mapper.ItemMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemCacheServiceImpl implements ItemCacheService {

    private final ItemRepository itemRepository;

    @Cacheable(value = "item", key = "#id")
    @Transactional(readOnly = true)
    @Override
    public Mono<ItemCacheDto> findItem(long id) {
        log.debug("Cache: add item with id = {}.", id);
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id, "Item with id = %d not found.".formatted(id))))
                .map(ItemMapper::toItemCacheDto);
    }

    @Cacheable(
            value = "items-page",
            key = "T(ru.practicum.market.service.cache.util.KeyGenerator).generateKeyForItemsPage(#search, #pageable)"
    )
    @Transactional(readOnly = true)
    @Override
    public Mono<ItemsPageCacheDto> getItemsPage(String search, Pageable pageable) {
        log.debug("Cache: add items: {}.", KeyGenerator.generateKeyForItemsPage(search, pageable));
        Mono<List<Item>> itemsMono;
        Mono<Long> itemsCountMono;

        if (StringUtils.hasText(search)) {
            itemsMono = itemRepository
                    .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable)
                    .collectList();
            itemsCountMono = itemRepository
                    .countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search);
        } else {
            itemsMono = itemRepository.findAllBy(pageable).collectList();
            itemsCountMono = itemRepository.count();
        }

        return Mono.zip(itemsMono, itemsCountMono)
                .map(t -> ItemMapper.toItemsPage(t.getT1(), t.getT2()));
    }

    @Cacheable(
            value = "cart",
            key = "T(ru.practicum.market.service.cache.util.KeyGenerator).generateKeyForCart(#itemIds)"
    )
    @Transactional(readOnly = true)
    @Override
    public Mono<CartCacheDto> getItemsByIds(List<Long> itemIds) {
        log.debug("Cache: add items for cart: {}.", KeyGenerator.generateKeyForCart(itemIds));
        return itemRepository.findByIdIn(itemIds)
                .collectList()
                .map(ItemMapper::toCartCacheDto);
    }
}
