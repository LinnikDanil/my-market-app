package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
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
    public ItemResponseDto getItem(long id) {
        log.info("Request to fetch item with id={}", id);
        var item = findItem(id);
        var itemQuantity = cartItemRepository.findByItemId(id).map(CartItem::getQuantity).orElse(0);

        log.debug("Item {} has quantity {} in cart", id, itemQuantity);
        return ItemMapper.toItemResponseDto(item, itemQuantity);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponseDto getCart() {
        log.info("Request to fetch cart");
        var cartItems = cartItemRepository.findAllFetch();
        log.debug("Cart contains {} items", cartItems.size());
        return ItemMapper.toCart(cartItems);
    }

    @Override
    @Transactional
    public void updateItemsCountInCart(long itemId, CartAction action) {
        log.info("Updating cart for itemId={} with action={}", itemId, action);
        var cartItem = cartItemRepository.findByItemId(itemId).orElse(null);

        switch (action) {
            case PLUS -> incrementItemQuantityInCart(cartItem, itemId);
            case MINUS -> decrementItemQuantityInCart(cartItem, itemId);
            case DELETE -> deleteItemFromCart(cartItem, itemId);
        }
    }

    private void incrementItemQuantityInCart(CartItem cartItem, long itemId) {
        log.debug("Increment item {} in cart", itemId);
        if (cartItem == null) {
            var item = findItem(itemId);
            cartItem = new CartItem(item, 1);
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
        }

        cartItemRepository.save(cartItem);
        log.info("Item {} quantity increased to {}", itemId, cartItem.getQuantity());
    }

    private void decrementItemQuantityInCart(CartItem cartItem, long itemId) {
        log.debug("Decrement item {} in cart", itemId);
        if (cartItem == null) {
            throw new CartItemNotFoundException(itemId, "Cart item with id = %d not found.".formatted(itemId));
        } else {
            var itemQuantity = cartItem.getQuantity();
            log.debug("Current quantity for item {} is {}", itemId, itemQuantity);
            if (itemQuantity == 1) {
                cartItemRepository.delete(cartItem);
                log.info("Item {} has been removed from cart", itemId);
            } else {
                cartItem.setQuantity(cartItem.getQuantity() - 1);
                cartItemRepository.save(cartItem);
                log.info("Item {} new quantity is {}", itemId, cartItem.getQuantity());
            }
        }
    }

    private void deleteItemFromCart(CartItem cartItem, long itemId) {
        log.debug("Deleting item {} from cart", itemId);
        if (cartItem == null) {
            throw new CartItemNotFoundException(itemId, "Cart item with id = %d not found.".formatted(itemId));
        }
        cartItemRepository.delete(cartItem);
        log.info("Item {} has been deleted from cart", itemId);
    }

    private Item findItem(long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id, "Item with id = %d not found.".formatted(id)));
    }


    private Paging convertToPaging(List<Item> items, long itemsCount, Pageable pageable) {
        var pageNumber = pageable.getPageNumber() + 1;
        var hasPreviousPage = pageNumber > 1;
        var hasNextPage = pageable.getOffset() + items.size() < itemsCount;

        return new Paging(items.size(), pageNumber, hasPreviousPage, hasNextPage);
    }
}
