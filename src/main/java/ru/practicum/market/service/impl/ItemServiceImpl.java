package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetrySpec;
import ru.practicum.market.domain.exception.CartItemNotFoundException;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.repository.CartItemRepository;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.web.dto.CartResponseDto;
import ru.practicum.market.web.dto.ItemResponseDto;
import ru.practicum.market.web.dto.ItemsResponseDto;
import ru.practicum.market.web.dto.Paging;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.dto.enums.SortMethod;
import ru.practicum.market.web.mapper.ItemMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ru.practicum.market.web.dto.enums.SortMethod.ALPHA;
import static ru.practicum.market.web.dto.enums.SortMethod.PRICE;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {

    private static final int ITEMS_IN_ROW = 3;

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public Mono<ItemsResponseDto> getItems(String search, SortMethod sortMethod, int pageNumber, int pageSize) {

        log.debug("Request to fetch items with search='{}', sortMethod={}, pageNumber={}, pageSize={}",
                search, sortMethod, pageNumber, pageSize);

        var sort = switch (sortMethod) {
            case NO -> Sort.unsorted();
            case ALPHA -> Sort.by(ALPHA.getColumnName());
            case PRICE -> Sort.by(PRICE.getColumnName());
        };
        var pageable = PageRequest.of(pageNumber - 1, pageSize, sort);

        Mono<List<Item>> itemsMono;
        Mono<Long> itemsCountMono;
        if (StringUtils.hasText(search)) {
            itemsMono = itemRepository
                    .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable)
                    .collectList();
            itemsCountMono = itemRepository
                    .countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search);
        } else {
            itemsMono = itemRepository.findAll(pageable).collectList();
            itemsCountMono = itemRepository.count();
        }

        return Mono.zip(itemsMono, itemsCountMono)
                .flatMap(tuple -> {
                    var items = tuple.getT1();
                    var itemsCount = tuple.getT2();

                    Mono<Map<Long, Integer>> quantityForItemMono = items.isEmpty()
                            ? Mono.just(Map.of())
                            : cartItemRepository.findByItemIdIn(items.stream().map(Item::getId).toList())
                            .collectMap(CartItem::getItemId, CartItem::getQuantity);

                    return quantityForItemMono.map(quantityForItem -> {
                        log.debug("Fetched {} items, {} related cart items", items.size(), quantityForItem.size());
                        var itemRows = ItemMapper.toItemRows(items, quantityForItem, ITEMS_IN_ROW);

                        log.debug("Items response prepared with {} rows", itemRows.size());

                        var paging = convertToPaging(items, itemsCount, pageable);

                        return new ItemsResponseDto(itemRows, search, sortMethod, paging);
                    });
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<ItemResponseDto> getItem(long id) {
        log.debug("Request to fetch item with id={}", id);

        var itemMono = findItem(id);
        var itemQuantityMono = cartItemRepository.findByItemId(id)
                .map(CartItem::getQuantity)
                .defaultIfEmpty(0);

        return Mono.zip(itemMono, itemQuantityMono)
                .map(tuple -> {
                    var item = tuple.getT1();
                    var itemQuantity = tuple.getT2();

                    log.debug("Item {} has quantity {} in cart", id, itemQuantity);

                    return ItemMapper.toItemResponseDto(item, itemQuantity);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<CartResponseDto> getCart() {
        log.debug("Request to fetch cart");

        return cartItemRepository.findAll().collectList()
                .flatMap(cartItems -> {
                    log.debug("Cart contains {} items", cartItems.size());

                    if (cartItems.isEmpty()) {
                        return Mono.just(new CartResponseDto(Collections.emptyList(), 0));
                    }

                    var itemIds = cartItems.stream().map(CartItem::getItemId).toList();
                    return itemRepository.findByIdIn(itemIds)
                            .collectList()
                            .map(items -> ItemMapper.toCart(cartItems, items));
                });
    }

    @Override
    @Transactional
    public Mono<Void> updateItemsCountInCart(long itemId, CartAction action) {
        log.debug("Updating cart for itemId={} with action={}", itemId, action);
        return switch (action) {
            case PLUS -> incrementItemQuantityInCart(itemId);
            case MINUS -> decrementItemQuantityInCart(itemId);
            case DELETE -> deleteItemFromCart(itemId);
        };
    }

    private Mono<Void> incrementItemQuantityInCart(long itemId) {
        log.debug("Increment item {} in cart", itemId);
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(itemId,
                        "Item with id = %d not found".formatted(itemId)))
                )
                .flatMap(exists -> cartItemRepository.findByItemId(itemId)
                        .defaultIfEmpty(new CartItem(itemId))
                        .flatMap(ci -> {
                            ci.setQuantity(ci.getQuantity() + 1);
                            return cartItemRepository.save(ci);
                        })
                        .retryWhen(getRetrySpec(itemId))
                        .doOnSuccess(ci -> log.debug("Item {} quantity increased to {}",
                                itemId, ci.getQuantity()))
                        .then()
                );
    }

    private Mono<Void> decrementItemQuantityInCart(long itemId) {
        log.debug("Decrement item {} in cart", itemId);
        return cartItemRepository.findByItemId(itemId)
                .switchIfEmpty(
                        Mono.error(new CartItemNotFoundException(itemId, "Cart item with id = %d not found."
                                .formatted(itemId)))
                )
                .flatMap(ci -> {
                    var itemQuantity = ci.getQuantity();
                    log.debug("Current quantity for item {} is {}", itemId, itemQuantity);

                    if (itemQuantity == 1) {
                        return cartItemRepository.delete(ci)
                                .doOnSuccess(v -> log.debug("Item {} has been removed from cart", itemId));
                    }

                    ci.setQuantity(ci.getQuantity() - 1);
                    return cartItemRepository.save(ci)
                            .doOnSuccess(saved -> log.debug("Item {} new quantity is {}",
                                    itemId, ci.getQuantity())
                            )
                            .then();

                })
                .retryWhen(getRetrySpec(itemId));


    }

    private Mono<Void> deleteItemFromCart(long itemId) {
        log.debug("Deleting item {} from cart", itemId);
        return cartItemRepository.findByItemId(itemId)
                .switchIfEmpty(Mono.error(
                        new CartItemNotFoundException(itemId, "Cart item with id = %d not found.".formatted(itemId)))
                ).flatMap(ci ->
                        cartItemRepository.delete(ci)
                                .doOnSuccess(v -> log.debug("Item {} has been deleted from cart", itemId))
                );
    }

    private RetrySpec getRetrySpec(long itemId) {
        return Retry
                .max(3)
                .filter(ex -> ex instanceof OptimisticLockingFailureException)
                .doBeforeRetry(retrySignal ->
                        log.warn("Optimistic lock conflict for itemId = {}, retry #{}",
                                itemId, retrySignal.totalRetries() + 1));
    }

    private Mono<Item> findItem(long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id, "Item with id = %d not found.".formatted(id))));
    }

    private Paging convertToPaging(List<Item> items, long itemsCount, Pageable pageable) {
        var pageNumber = pageable.getPageNumber() + 1;
        var hasPreviousPage = pageNumber > 1;
        var hasNextPage = pageable.getOffset() + items.size() < itemsCount;

        return new Paging(items.size(), pageNumber, hasPreviousPage, hasNextPage);
    }
}
