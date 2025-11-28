package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
@Log4j2
public class ItemServiceImpl implements ItemService {

    private static final int ITEMS_IN_ROW = 3;

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public ItemsResponseDto getItems(String search, SortMethod sortMethod, int pageNumber, int pageSize) {

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

        var itemsQuantity = cartItems.stream()
                .collect(Collectors.toMap(
                        ci -> ci.getItem().getId(),
                        CartItem::getQuantity
                ));

        var itemRows = ItemMapper.toItemRows(items, itemsQuantity, ITEMS_IN_ROW);

        return new ItemsResponseDto(itemRows, search, sortMethod, convertToPaging(itemsPage));
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponseDto getItem(long id) {
        var item = findItem(id);
        var itemQuantity = cartItemRepository.findByItemId(id).map(CartItem::getQuantity).orElse(0);

        return ItemMapper.toItemResponseDto(item, itemQuantity);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponseDto getCart() {
        return ItemMapper.toCart(
                cartItemRepository.findAllFetch()
        );
    }

    @Override
    @Transactional
    public void updateItemsCountInCart(long itemId, CartAction action) {
        var cartItem = cartItemRepository.findByItemId(itemId).orElse(null);

        switch (action) {
            case PLUS -> incrementItemQuantityInCart(cartItem, itemId);
            case MINUS -> decrementItemQuantityInCart(cartItem, itemId);
            case DELETE -> deleteItemFromCart(cartItem, itemId);
        }
    }

    private void incrementItemQuantityInCart(CartItem cartItem, long itemId) {
        log.debug("Increment item in cart");
        if (cartItem == null) {
            var item = findItem(itemId);
            cartItem = new CartItem(item, 1);
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
        }

        cartItemRepository.save(cartItem);
    }

    private void decrementItemQuantityInCart(CartItem cartItem, long itemId) {
        log.debug("Decrement item in cart");
        if (cartItem == null) {
            throw new CartItemNotFoundException("Cart item with id = %d not found.".formatted(itemId));
        } else {

            var itemQuantity = cartItem.getQuantity();
            log.debug("current quantity is {}", itemQuantity);
            if (itemQuantity == 1) {
                cartItemRepository.delete(cartItem);
                log.debug("item has been deleted from cart");
            } else {
                cartItem.setQuantity(cartItem.getQuantity() - 1);
                cartItemRepository.save(cartItem);
                log.debug("item new size = {}", cartItem.getQuantity());
            }
        }
    }

    private void deleteItemFromCart(CartItem cartItem, long itemId) {
        log.debug("Deleting item from cart");
        if (cartItem == null) {
            throw new CartItemNotFoundException("Cart item with id = %d not found.".formatted(itemId));
        }
        cartItemRepository.delete(cartItem);
        log.debug("item has been deleted from cart");
    }

    private Item findItem(long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item with id = %d not found.".formatted(id)));
    }

    private Paging convertToPaging(Page<Item> page) {
        return new Paging(page.getSize(), page.getNumber() + 1, page.hasPrevious(), page.hasNext());
    }
}
