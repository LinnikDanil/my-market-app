package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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

import java.util.stream.Collectors;

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
    public ItemsResponseDto getItems(String search, SortMethod sortMethod, int pageNumber, int pageSize) {

        log.info("Request to fetch items with search='{}', sortMethod={}, pageNumber={}, pageSize={}",
                search, sortMethod, pageNumber, pageSize);

        var sort = switch (sortMethod) {
            case NO -> Sort.unsorted();
            case ALPHA -> Sort.by(ALPHA.getColumnName());
            case PRICE -> Sort.by(PRICE.getColumnName());
        };
        var pageable = PageRequest.of(pageNumber - 1, pageSize, sort);

        Page<Item> itemsPage;
        if (StringUtils.hasText(search)) {
            itemsPage = itemRepository
                    .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
        } else {
            itemsPage = itemRepository.findAll(pageable);
        }

        var items = itemsPage.getContent();
        var cartItems = cartItemRepository.findByItemIds(items.stream().map(Item::getId).toList());

        log.debug("Fetched {} items, {} related cart items", items.size(), cartItems.size());

        var itemsQuantity = cartItems.stream()
                .collect(Collectors.toMap(
                        ci -> ci.getItem().getId(),
                        CartItem::getQuantity
                ));

        var itemRows = ItemMapper.toItemRows(items, itemsQuantity, ITEMS_IN_ROW);

        var response = new ItemsResponseDto(itemRows, search, sortMethod, convertToPaging(itemsPage));
        log.info("Items response prepared with {} rows", itemRows.size());
        return response;
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

    private Paging convertToPaging(Page<Item> page) {
        return new Paging(page.getSize(), page.getNumber() + 1, page.hasPrevious(), page.hasNext());
    }
}
