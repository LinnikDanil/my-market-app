package ru.practicum.market.service.cache.util;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class KeyGenerator {
    /**
     * Генерирует ключ кэша для страницы товаров.
     */
    public static String generateKeyForItemsPage(String search, Pageable pageable) {
        return (search == null || search.isBlank() ? "_" : search)
                + "|" + pageable.getSort()
                + "|" + pageable.getPageNumber()
                + "|" + pageable.getPageSize();
    }

    /**
     * Генерирует ключ кэша корзины на основе отсортированного списка id.
     */
    public static String generateKeyForCart(List<Long> ids) {
        return ids.stream()
                .sorted()
                .map(Object::toString)
                .collect(Collectors.joining("|"));
    }
}
