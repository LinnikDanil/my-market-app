package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.web.dto.ItemsResponseDto;
import ru.practicum.market.web.dto.Paging;
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

    private Paging convertToPaging(Page<Item> page) {
        return new Paging(page.getSize(), page.getNumber(), page.hasPrevious(), page.hasNext());
    }
}
