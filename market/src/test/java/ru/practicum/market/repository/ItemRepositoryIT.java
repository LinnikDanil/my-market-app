package ru.practicum.market.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.market.util.PostgresContainer;
import ru.practicum.market.util.TestCacheConfig;
import ru.practicum.market.util.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Testcontainers
@ImportTestcontainers(PostgresContainer.class)
@DisplayName("ItemRepository")
@Import(TestCacheConfig.class)
class ItemRepositoryIT {

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    DatabaseClient db;

    @BeforeEach
    void clean() {
        db.sql("TRUNCATE TABLE items RESTART IDENTITY CASCADE").then().block();
    }
    
    @Test
    @DisplayName("search all containing")
    void test1() {
        var items = TestDataFactory.createItemsForSave(3);
        var firstItem = items.getFirst();
        var search = "title";
        var pageable = PageRequest.of(0, 10, Sort.unsorted());

        var resultMono = itemRepository.saveAll(items)
                .thenMany(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        search, search, pageable
                ))
                .collectList();

        var result = resultMono.block();
        assertThat(result).isNotNull();
        assertThat(result)
                .isNotEmpty()
                .hasSize(items.size());

        var resultFirstItem = result.getFirst();
        assertThat(resultFirstItem.getId()).isGreaterThan(0L);
        assertThat(resultFirstItem.getTitle()).isEqualTo(firstItem.getTitle());
        assertThat(resultFirstItem.getDescription()).isEqualTo(firstItem.getDescription());
        assertThat(resultFirstItem.getImgPath()).isEqualTo(firstItem.getImgPath());
        assertThat(resultFirstItem.getPrice()).isEqualTo(firstItem.getPrice());
    }

    @Test
    @DisplayName("search one containing")
    void test2() {
        var items = TestDataFactory.createItemsForSave(3);
        var firstItem = items.getFirst();
        var search = "title1";
        var pageable = PageRequest.of(0, 10, Sort.unsorted());

        var resultMono = itemRepository.saveAll(items)
                .thenMany(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        search, search, pageable
                ))
                .collectList();

        var result = resultMono.block();
        assertThat(result).isNotNull();
        assertThat(result)
                .isNotEmpty()
                .hasSize(1);

        var resultFirstItem = result.getFirst();
        assertThat(resultFirstItem.getId()).isGreaterThan(0L);
        assertThat(resultFirstItem.getTitle()).isEqualTo(firstItem.getTitle());
        assertThat(resultFirstItem.getDescription()).isEqualTo(firstItem.getDescription());
        assertThat(resultFirstItem.getImgPath()).isEqualTo(firstItem.getImgPath());
        assertThat(resultFirstItem.getPrice()).isEqualTo(firstItem.getPrice());
    }

    @Test
    @DisplayName("not containing")
    void test3() {
        var items = TestDataFactory.createItemsForSave(3);
        var search = "title1005001000120312030123";
        var pageable = PageRequest.of(0, 10, Sort.unsorted());

        var resultMono = itemRepository.saveAll(items)
                .thenMany(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        search, search, pageable
                ))
                .collectList();

        var result = resultMono.block();
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
}
