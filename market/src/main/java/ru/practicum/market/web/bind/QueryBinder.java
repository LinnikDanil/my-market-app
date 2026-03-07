package ru.practicum.market.web.bind;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import ru.practicum.market.domain.exception.MarketBadRequestException;
import ru.practicum.market.web.bind.model.ItemsQuery;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.dto.enums.SortMethod;

/**
 * Компонент парсинга и валидации query/path-параметров входящих HTTP-запросов.
 */
@Component
@Slf4j
public class QueryBinder {
    private static final String PATH_VARIABLE_ID = "id";
    private static final String PARAM_ID = "id";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_SEARCH = "search";
    private static final String PARAM_SORT = "sort";
    private static final String PARAM_PAGE_NUMBER = "pageNumber";
    private static final String PARAM_PAGE_SIZE = "pageSize";
    private static final String PARAM_NEW_ORDER = "newOrder";
    private static final int MIN_PAGE_NUMBER = 1;
    private static final int MIN_PAGE_SIZE = 5;

    /**
     * Собирает параметры списка товаров из query-параметров запроса.
     *
     * @param request входящий HTTP-запрос
     * @return объект с параметрами поиска, сортировки и пагинации
     */
    public ItemsQuery bindItemsQuery(ServerRequest request) {
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

        var query = new ItemsQuery(search, sortMethod, pageNumber, pageSize);
        log.debug("Bound items query: search='{}', sort={}, pageNumber={}, pageSize={}",
                query.search(), query.sort(), query.pageNumber(), query.pageSize());
        return query;
    }

    /**
     * Извлекает и валидирует id из path-переменной.
     *
     * @param request входящий HTTP-запрос
     * @return идентификатор сущности
     */
    public long bindPathVariableId(ServerRequest request) {
        var id = request.pathVariable(PATH_VARIABLE_ID);
        var parsedId = parseId(id);
        log.debug("Bound path id={}", parsedId);
        return parsedId;
    }

    /**
     * Извлекает и валидирует id из query-параметра.
     *
     * @param request входящий HTTP-запрос
     * @return идентификатор сущности
     */
    public long bindParamId(ServerRequest request) {
        var id = request.queryParam(PARAM_ID)
                .map(this::parseId)
                .orElseThrow(() -> new MarketBadRequestException("Missing query param: " + PARAM_ID));
        log.debug("Bound query id={}", id);
        return id;
    }

    /**
     * Извлекает действие корзины из query-параметра.
     *
     * @param request входящий HTTP-запрос
     * @return действие корзины
     */
    public CartAction bindParamAction(ServerRequest request) {
        var action = request.queryParam(PARAM_ACTION)
                .map(ca -> {
                    try {
                        return CartAction.valueOf(ca);
                    } catch (IllegalArgumentException e) {
                        throw new MarketBadRequestException("CartAction not found " + ca);
                    }
                })
                .orElseThrow(() -> new MarketBadRequestException("Missing query param: " + PARAM_ACTION));
        log.debug("Bound cart action={}", action);
        return action;
    }

    /**
     * Извлекает флаг "новый заказ" из query-параметра.
     *
     * @param request входящий HTTP-запрос
     * @return флаг нового заказа
     */
    public boolean bindParamNewOrder(ServerRequest request) {
        var isNewOrder = request.queryParam(PARAM_NEW_ORDER)
                .map(newOrder -> {
                    try {
                        return Boolean.parseBoolean(newOrder);
                    } catch (IllegalArgumentException e) {
                        throw new MarketBadRequestException("Boolean not true or false: " + newOrder);
                    }
                })
                .orElse(false);
        log.debug("Bound newOrder={}", isNewOrder);
        return isNewOrder;
    }

    /**
     * Парсит положительное целое значение.
     *
     * @param pg    исходная строка
     * @param field имя параметра
     * @return положительное целое значение
     */
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

    /**
     * Парсит значение сортировки.
     *
     * @param sort строковое значение сортировки
     * @return enum значения сортировки
     */
    private SortMethod parseSort(String sort) {
        try {
            return SortMethod.valueOf(sort);
        } catch (IllegalArgumentException e) {
            throw new MarketBadRequestException("sort unknown value: %s".formatted(sort));
        }
    }

    /**
     * Парсит id в long.
     *
     * @param id исходная строка
     * @return числовой идентификатор
     */
    private long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new MarketBadRequestException("id = %s should be long".formatted(id));
        }
    }
}
