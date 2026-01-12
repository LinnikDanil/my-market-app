package ru.practicum.market.service.cache.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.service.cache.ItemCacheService;
import ru.practicum.market.util.PostgresContainer;
import ru.practicum.market.util.RedisTestContainer;
import ru.practicum.market.util.TestDataFactory;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ImportTestcontainers({RedisTestContainer.class, PostgresContainer.class})
@DisplayName("ItemCacheServiceImpl")
class ItemCacheServiceIT {

    @Autowired
    private ItemCacheService itemCacheService;

    @MockitoSpyBean
    private ItemRepository itemRepository;

    @Nested
    @DisplayName("findItem")
    class FindItem {

        @Test
        @DisplayName("cached with ttl")
        void test1() throws InterruptedException {
            var item = TestDataFactory.createItemForSave(1);
            var savedItem = itemRepository.save(item).block();
            var firstResponse = itemCacheService.findItem(savedItem.getId()).block();
            var secondResponse = itemCacheService.findItem(savedItem.getId()).block();

            assertThat(firstResponse).isNotNull();
            assertThat(secondResponse).isNotNull();
            assertThat(secondResponse.id()).isEqualTo(savedItem.getId());

            verify(itemRepository, times(1)).findById(savedItem.getId());

            TimeUnit.SECONDS.sleep(2L);

            var thirdResponse = itemCacheService.findItem(savedItem.getId()).block();
            assertThat(thirdResponse).isNotNull();
            assertThat(thirdResponse.id()).isEqualTo(savedItem.getId());

            verify(itemRepository, times(2)).findById(savedItem.getId());
        }
    }

    @Nested
    @DisplayName("getItemsByIds")
    class GetItemsByIds {

        @Test
        @DisplayName("cached by key")
        void test1() throws InterruptedException {
            var items = TestDataFactory.createItemsForSave(2);
            var savedItem = itemRepository.saveAll(items);

            var itemIds = savedItem.map(Item::getId).collectList().block();

            var firstResponse = itemCacheService.getItemsByIds(itemIds).block();
            var secondResponse = itemCacheService.getItemsByIds(itemIds).block();

            assertThat(firstResponse).isNotNull();
            assertThat(secondResponse).isNotNull();
            assertThat(secondResponse.items())
                    .extracting("id")
                    .containsExactlyElementsOf(itemIds);

            verify(itemRepository, times(1)).findByIdIn(itemIds);

            TimeUnit.SECONDS.sleep(2L);

            var thirdResponse = itemCacheService.getItemsByIds(itemIds).block();
            assertThat(thirdResponse).isNotNull();

            verify(itemRepository, times(2)).findByIdIn(itemIds);
        }
    }

    @Nested
    @DisplayName("getItemsPage")
    class GetItemsPage {

        @Test
        @DisplayName("cache without search")
        void test1() {
            var items = TestDataFactory.createItems(3);
            var pageable = PageRequest.of(0, 2);

            when(itemRepository.findAllBy(pageable)).thenReturn(Flux.fromIterable(items));
            when(itemRepository.count()).thenReturn(Mono.just(3L));

            var firstResponse = itemCacheService.getItemsPage(null, pageable).block();
            var secondResponse = itemCacheService.getItemsPage(null, pageable).block();

            assertThat(firstResponse).isNotNull();
            assertThat(secondResponse).isNotNull();
            assertThat(secondResponse.items()).hasSize(3);
            assertThat(secondResponse.itemsCount()).isEqualTo(3L);

            verify(itemRepository, times(1)).findAllBy(pageable);
            verify(itemRepository, times(1)).count();
        }

        @Test
        @DisplayName("cache with search")
        void test2() {
            var search = "title";
            var items = TestDataFactory.createItems(2);
            Pageable pageable = PageRequest.of(0, 5);

            when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    eq(search),
                    eq(search),
                    eq(pageable)
            )).thenReturn(Flux.fromIterable(items));
            when(itemRepository.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search,
                    search
            )).thenReturn(Mono.just(2L));

            var firstResponse = itemCacheService.getItemsPage(search, pageable).block();
            var secondResponse = itemCacheService.getItemsPage(search, pageable).block();

            assertThat(firstResponse).isNotNull();
            assertThat(secondResponse).isNotNull();
            assertThat(secondResponse.items()).hasSize(2);
            assertThat(secondResponse.itemsCount()).isEqualTo(2L);

            verify(itemRepository, times(1))
                    .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
            verify(itemRepository, times(1))
                    .countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search);
        }
    }
}
