package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetrySpec;
import ru.practicum.market.domain.exception.CartItemNotFoundException;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.integration.PaymentAdapter;
import ru.practicum.market.repository.CartItemRepository;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.service.cache.ItemCacheService;
import ru.practicum.market.service.cache.dto.ItemCacheDto;
import ru.practicum.market.web.dto.CartResponseDto;
import ru.practicum.market.web.dto.ItemResponseDto;
import ru.practicum.market.web.dto.ItemsResponseDto;
import ru.practicum.market.web.dto.Paging;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.dto.enums.SortMethod;
import ru.practicum.market.web.mapper.ItemMapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ru.practicum.market.web.dto.enums.SortMethod.ALPHA;
import static ru.practicum.market.web.dto.enums.SortMethod.PRICE;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {

    private static final int ITEMS_IN_ROW = 3;

    private final ItemCacheService itemCacheService;
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentAdapter paymentAdapter;

    /**
     * Возвращает страницу товаров с учетом поиска, сортировки и количества в корзине.
     */
    @Override
    @Transactional(readOnly = true)
    public Mono<ItemsResponseDto> getItems(Optional<Long> userIdOpt, String search, SortMethod sortMethod, int pageNumber, int pageSize) {

        log.debug("Request to fetch items with search='{}', sortMethod={}, pageNumber={}, pageSize={}",
                search, sortMethod, pageNumber, pageSize);

        var pageable = buildPageable(sortMethod, pageNumber, pageSize);

        return itemCacheService.getItemsPage(search, pageable)
                .flatMap(itemsPage ->
                        buildItemsResponse(userIdOpt, search, sortMethod, pageable, itemsPage.items(), itemsPage.itemsCount()));
    }

    /**
     * Возвращает карточку товара и его текущее количество в корзине.
     */
    @Override
    @Transactional(readOnly = true)
    public Mono<ItemResponseDto> getItem(Optional<Long> userIdOpt, long itemId) {
        log.debug("Request to fetch item with itemId={}", itemId);

        return itemCacheService.findItem(itemId)
                .flatMap(item ->
                        userIdOpt.map(userId ->
                                        cartItemRepository.findByUserIdAndItemId(userId, itemId)
                                                .map(CartItem::getQuantity)
                                                .defaultIfEmpty(0)
                                )
                                .orElse(Mono.just(0))
                                .map(itemQuantity -> ItemMapper.toItemResponseDto(item, itemQuantity))
                                .doOnSuccess(r -> log.debug("Item {} has quantity {} in cart", itemId, r.count()))
                );
    }

    /**
     * Возвращает корзину вместе с признаком доступности оплаты по текущему балансу.
     */
    @Override
    @Transactional(readOnly = true)
    public Mono<CartResponseDto> getCart(Long userId) {
        log.debug("Request to fetch cart for userId={}", userId);

        return cartItemRepository.findByUserId(userId).collectList()
                .flatMap(cartItems -> {
                    log.debug("Cart contains {} items for userId={}", cartItems.size(), userId);

                    if (cartItems.isEmpty()) {
                        return Mono.just(new CartResponseDto(Collections.emptyList(), 0, false));
                    }

                    var itemIds = cartItems.stream().map(CartItem::getItemId).toList();


                    var cartCacheMono = itemCacheService.getItemsByIds(itemIds);
                    var currentBalanceMono = paymentAdapter.getBalance(userId);

                    return Mono.zip(cartCacheMono, currentBalanceMono)
                            .map(t -> {
                                var cartCache = t.getT1();
                                var currentBalance = t.getT2().getBalance();
                                return ItemMapper.toCart(cartItems, cartCache.items(), currentBalance);
                            });
                });
    }

    /**
     * Возвращает корзину без проверки баланса во внешнем сервисе платежей.
     */
    @Transactional(readOnly = true)
    @Override
    public Mono<CartResponseDto> getCartWithoutPayments(long userId) {
        log.debug("Request to fetch cart");

        return cartItemRepository.findByUserId(userId)
                .collectList()
                .flatMap(cartItems -> {
                    log.debug("Cart contains {} items", cartItems.size());

                    if (cartItems.isEmpty()) {
                        return Mono.just(new CartResponseDto(Collections.emptyList(), 0, false));
                    }

                    var itemIds = cartItems.stream().map(CartItem::getItemId).toList();
                    return itemCacheService.getItemsByIds(itemIds)
                            .map(cartCache -> ItemMapper.toCart(cartItems, cartCache.items(), BigDecimal.ZERO));
                });
    }

    /**
     * Обновляет количество товара в корзине по типу действия.
     */
    @Override
    @Transactional
    public Mono<Void> updateItemsCountInCart(long userId, long itemId, CartAction action) {
        log.debug("Updating cart for itemId={} with action={}", itemId, action);
        return switch (action) {
            case PLUS -> incrementItemQuantityInCart(userId, itemId);
            case MINUS -> decrementItemQuantityInCart(userId, itemId);
            case DELETE -> deleteItemFromCart(userId, itemId);
        };
    }

    /**
     * Увеличивает количество товара в корзине на единицу.
     */
    private Mono<Void> incrementItemQuantityInCart(long userId, long itemId) {
        log.debug("Increment item {} in cart", itemId);
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(itemId,
                        "Item with id = %d not found".formatted(itemId)))
                )
                .flatMap(exists -> cartItemRepository.findByUserIdAndItemId(userId, itemId)
                        .defaultIfEmpty(new CartItem(userId, itemId))
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

    /**
     * Уменьшает количество товара в корзине на единицу либо удаляет позицию при количестве 1.
     */
    private Mono<Void> decrementItemQuantityInCart(long userId, long itemId) {
        log.debug("Decrement item {} in cart", itemId);
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
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

    /**
     * Полностью удаляет товар из корзины.
     */
    private Mono<Void> deleteItemFromCart(long userId, long itemId) {
        log.debug("Deleting item {} from cart", itemId);
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .switchIfEmpty(Mono.error(
                        new CartItemNotFoundException(itemId, "Cart item with id = %d not found.".formatted(itemId)))
                ).flatMap(ci ->
                        cartItemRepository.delete(ci)
                                .doOnSuccess(v -> log.debug("Item {} has been deleted from cart", itemId))
                );
    }

    /**
     * Возвращает retry-стратегию для конфликтов оптимистической блокировки.
     */
    private RetrySpec getRetrySpec(long itemId) {
        return Retry
                .max(3)
                .filter(ex -> ex instanceof OptimisticLockingFailureException)
                .doBeforeRetry(retrySignal ->
                        log.warn("Optimistic lock conflict for itemId = {}, retry #{}",
                                itemId, retrySignal.totalRetries() + 1));
    }

    /**
     * Формирует DTO пагинации для UI.
     */
    private Paging convertToPaging(long itemsSize, long itemsCount, Pageable pageable) {
        var pageNumber = pageable.getPageNumber() + 1;
        var hasPreviousPage = pageNumber > 1;
        var hasNextPage = pageable.getOffset() + itemsSize < itemsCount;
        log.debug("Paging calculated: pageNumber={}, pageSize={}, offset={}, itemsOnPage={}, itemsTotal={}, hasPrevious={}, hasNext={}",
                pageNumber, pageable.getPageSize(), pageable.getOffset(), itemsSize, itemsCount, hasPreviousPage, hasNextPage);

        return new Paging(pageable.getPageSize(), pageNumber, hasPreviousPage, hasNextPage);
    }

    /**
     * Создает параметры пагинации и сортировки.
     */
    private Pageable buildPageable(SortMethod sortMethod, int pageNumber, int pageSize) {
        var sort = switch (sortMethod) {
            case NO -> Sort.unsorted();
            case ALPHA -> Sort.by(ALPHA.getColumnName());
            case PRICE -> Sort.by(PRICE.getColumnName());
        };
        return PageRequest.of(pageNumber - 1, pageSize, sort);
    }

    /**
     * Формирует DTO страницы товаров с учетом количеств в корзине.
     */
    private Mono<ItemsResponseDto> buildItemsResponse(
            Optional<Long> userId,
            String search,
            SortMethod sortMethod,
            Pageable pageable,
            List<ItemCacheDto> items,
            long itemsCount
    ) {
        return getQuantityForItems(userId, items)
                .map(quantityForItem -> {
                    log.debug("Fetched {} items, {} related cart items", items.size(), quantityForItem.size());
                    var itemRows = ItemMapper.toItemRows(items, quantityForItem, ITEMS_IN_ROW);
                    log.debug("Items response prepared with {} rows", itemRows.size());
                    var paging = convertToPaging(items.size(), itemsCount, pageable);
                    return new ItemsResponseDto(itemRows, search, sortMethod, paging);
                });
    }

    /**
     * Загружает количество товаров в корзине по списку элементов страницы.
     */
    private Mono<Map<Long, Integer>> getQuantityForItems(Optional<Long> userId, List<ItemCacheDto> items) {
        if (items.isEmpty() || userId.isEmpty()) {
            return Mono.just(Map.of());
        }

        var itemIds = items.stream().map(ItemCacheDto::id).toList();
        return cartItemRepository.findByUserIdAndItemIdIn(userId.get(), itemIds)
                .collectMap(CartItem::getItemId, CartItem::getQuantity);
    }
}
