package ru.practicum.market.web.bind;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import ru.practicum.market.domain.exception.MarketBadRequestException;
import ru.practicum.market.web.bind.model.ItemsQuery;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.dto.enums.SortMethod;

@Component
public class ItemsQueryBinder {
    private static final String PATH_VARIABLE_ID = "id";
    private static final String PARAM_ID = "id";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_SEARCH = "search";
    private static final String PARAM_SORT = "sort";
    private static final String PARAM_PAGE_NUMBER = "pageNumber";
    private static final String PARAM_PAGE_SIZE = "pageSize";
    private static final int MIN_PAGE_NUMBER = 1;
    private static final int MIN_PAGE_SIZE = 5;

    public ItemsQuery bind(ServerRequest request) {
        String search = request.queryParam(PARAM_SEARCH)
                .orElse(null);
        var sortMethod = request.queryParam(PARAM_SORT)
                .map(this::parseSort)
                .orElse(SortMethod.NO);
        var pageNumber = request.queryParam(PARAM_PAGE_NUMBER)
                .map(pg -> parsePositiveInt(pg, PARAM_PAGE_NUMBER))
                .orElse(MIN_PAGE_NUMBER);
        var pageSize = request.queryParam(PARAM_PAGE_SIZE)
                .map(pg -> parsePositiveInt(pg, PARAM_PAGE_SIZE))
                .orElse(MIN_PAGE_SIZE);

        return new ItemsQuery(search, sortMethod, pageNumber, pageSize);
    }

    public long bindPathVariableId(ServerRequest request) {
        var id = request.pathVariable(PATH_VARIABLE_ID);
        return parseId(id);
    }

    public long bindParamId(ServerRequest request) {
        return request.queryParam(PARAM_ID)
                .map(this::parseId)
                .orElseThrow(() -> new MarketBadRequestException("Missing query param: " + PARAM_ID));
    }

    public CartAction bindParamAction(ServerRequest request) {
        return request.queryParam(PARAM_ACTION)
                .map(ca -> {
                    try {
                        return CartAction.valueOf(ca);
                    } catch (IllegalArgumentException e) {
                        throw new MarketBadRequestException("CartAction not found " + ca);
                    }
                })
                .orElseThrow(() -> new MarketBadRequestException("Missing query param: " + PARAM_ACTION));
    }

    private int parsePositiveInt(String pg, String field) {
        int value;
        try {
            value = Integer.parseInt(pg);
        } catch (NumberFormatException e) {
            throw new MarketBadRequestException("%s should be integer".formatted(field));
        }
        if (value < 1) {
            throw new MarketBadRequestException("%s should be greater than 0".formatted(field));
        }
        return value;
    }

    private SortMethod parseSort(String sort) {
        try {
            return SortMethod.valueOf(sort);
        } catch (IllegalArgumentException e) {
            throw new MarketBadRequestException("sort unknown value: %s".formatted(sort));
        }
    }

    private long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new MarketBadRequestException("id = %s should be long".formatted(id));
        }
    }
}
