package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.practicum.market.domain.exception.ItemCountInCartException;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.web.dto.ItemResponseDto;
import ru.practicum.market.web.dto.ItemsResponseDto;
import ru.practicum.market.web.dto.Paging;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.dto.enums.SortMethod;
import ru.practicum.market.web.mapper.ItemMapper;

import static ru.practicum.market.web.dto.enums.SortMethod.ALPHA;
import static ru.practicum.market.web.dto.enums.SortMethod.PRICE;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private static final int ITEMS_IN_ROW = 3;

    private final ItemRepository itemRepository;

    @Override
    @Transactional(readOnly = true)
    public ItemsResponseDto getItems(String search, SortMethod sortMethod, int pageNumber, int pageSize) {

        var sort = switch (sortMethod) {
            case NO -> Sort.unsorted();
            case ALPHA -> Sort.by(ALPHA.getColumnName());
            case PRICE -> Sort.by(PRICE.getColumnName());
        };
        var pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<Item> items;
        if (StringUtils.hasText(search)) {
            items = itemRepository
                    .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
        } else {
            items = itemRepository.findAll(pageable);
        }

        var itemRows = ItemMapper.toItemRows(items.getContent(), ITEMS_IN_ROW);

        return new ItemsResponseDto(itemRows, search, sortMethod, convertToPaging(items));
    }

    @Override
    public ItemResponseDto getItem(long id) {
        return itemRepository.findById(id)
                .map(ItemMapper::toItemResponseDto)
                .orElseThrow(() -> new ItemNotFoundException("Item with id = %d not found.".formatted(id)));
    }

    @Override
    @Transactional
    public void updateItemsCountInCart(long id, CartAction action) {
        try {
            var rowsUpdated = switch (action) {
                case PLUS -> itemRepository.incrementItemCount(id);
                case MINUS -> itemRepository.decrementItemCount(id);
            };

            if (rowsUpdated == 0) {
                throw new ItemNotFoundException("Item with id = %d not found.".formatted(id));
            }

        } catch (DataIntegrityViolationException exception) {
            throw new ItemCountInCartException("Count items with id = %d in cart should be greater 0.".formatted(id));
        }
    }

    private Paging convertToPaging(Page<Item> page) {
        return new Paging(page.getSize(), page.getNumber(), page.hasPrevious(), page.hasNext());
    }
}
