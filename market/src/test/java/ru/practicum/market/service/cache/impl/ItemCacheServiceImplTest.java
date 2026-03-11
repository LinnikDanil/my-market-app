package ru.practicum.market.service.cache.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.util.TestDataFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemCacheServiceImpl")
class ItemCacheServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemCacheServiceImpl service;

    @Nested
    @DisplayName("findItem")
    class FindItem {

        @Test
        @DisplayName("returns item from repository")
        void test1() {
            var item = TestDataFactory.createItem(1L);
            when(itemRepository.findById(1L)).thenReturn(Mono.just(item));

            var response = service.findItem(1L).block();

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.title()).isEqualTo("title1");
            assertThat(response.description()).isEqualTo("description1");
            assertThat(response.imgPath()).isEqualTo("imgPath1");
            assertThat(response.price()).isEqualTo(100L);
        }

        @Test
        @DisplayName("throws when item does not exist")
        void test2() {
            when(itemRepository.findById(99L)).thenReturn(Mono.empty());

            assertThatExceptionOfType(ItemNotFoundException.class)
                    .isThrownBy(() -> service.findItem(99L).block())
                    .withMessage("Item with id = 99 not found.");
        }
    }

    @Nested
    @DisplayName("getItemsPage")
    class GetItemsPage {

        @Test
        @DisplayName("uses search query when search has text")
        void test1() {
            var search = "title";
            Pageable pageable = PageRequest.of(0, 2);
            var items = TestDataFactory.createItems(2);

            when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    eq(search), eq(search), eq(pageable))
            ).thenReturn(Flux.fromIterable(items));
            when(itemRepository.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search))
                    .thenReturn(Mono.just(2L));

            var response = service.getItemsPage(search, pageable).block();

            assertThat(response).isNotNull();
            assertThat(response.items()).hasSize(2);
            assertThat(response.itemsCount()).isEqualTo(2L);
            verify(itemRepository, times(1))
                    .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
            verify(itemRepository, times(1))
                    .countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search);
            verify(itemRepository, never()).findAllBy(pageable);
            verify(itemRepository, never()).count();
        }

        @Test
        @DisplayName("uses all items query when search is blank")
        void test2() {
            var search = "   ";
            Pageable pageable = PageRequest.of(0, 5);
            var items = TestDataFactory.createItems(3);

            when(itemRepository.findAllBy(pageable)).thenReturn(Flux.fromIterable(items));
            when(itemRepository.count()).thenReturn(Mono.just(3L));

            var response = service.getItemsPage(search, pageable).block();

            assertThat(response).isNotNull();
            assertThat(response.items()).hasSize(3);
            assertThat(response.itemsCount()).isEqualTo(3L);
            verify(itemRepository, times(1)).findAllBy(pageable);
            verify(itemRepository, times(1)).count();
            verify(itemRepository, never())
                    .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
            verify(itemRepository, never())
                    .countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search);
        }

        @Test
        @DisplayName("propagates repository error")
        void test3() {
            Pageable pageable = PageRequest.of(0, 2);
            when(itemRepository.findAllBy(pageable)).thenReturn(Flux.error(new IllegalStateException("db error")));
            when(itemRepository.count()).thenReturn(Mono.just(0L));

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> service.getItemsPage(null, pageable).block())
                    .withMessage("db error");
        }
    }

    @Nested
    @DisplayName("getItemsByIds")
    class GetItemsByIds {

        @Test
        @DisplayName("returns items for cart")
        void test1() {
            var first = TestDataFactory.createItem(1L);
            var second = TestDataFactory.createItem(2L);
            var ids = List.of(1L, 2L);

            when(itemRepository.findByIdIn(ids)).thenReturn(Flux.just(first, second));

            var response = service.getItemsByIds(ids).block();

            assertThat(response).isNotNull();
            assertThat(response.items()).hasSize(2);
            assertThat(response.items())
                    .extracting("id")
                    .containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("propagates repository error")
        void test2() {
            var ids = List.of(1L);
            when(itemRepository.findByIdIn(ids)).thenReturn(Flux.error(new IllegalStateException("db error")));

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> service.getItemsByIds(ids).block())
                    .withMessage("db error");
        }
    }
}
