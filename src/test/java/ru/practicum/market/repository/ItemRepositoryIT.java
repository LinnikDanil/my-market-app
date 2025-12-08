package ru.practicum.market.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.market.util.PostgresContainer;
import ru.practicum.market.util.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ImportTestcontainers(PostgresContainer.class)
@DisplayName("ItemRepository")
class ItemRepositoryIT {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    @DisplayName("search all containing")
    void test1() {
        var items = TestDataFactory.createItemsForSave(3);
        var firstItem = items.getFirst();
        var search = "title";
        var pageable = PageRequest.of(0, 10, Sort.unsorted());
        itemRepository.saveAll(items);

        var result = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                search, search, pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .isNotEmpty()
                .hasSize(items.size());

        var resultFirstItem = result.getContent().getFirst();
        assertThat(resultFirstItem.getId()).isGreaterThan(0L);
        assertThat(resultFirstItem.getTitle()).isEqualTo(firstItem.getTitle());
        assertThat(resultFirstItem.getDescription()).isEqualTo(firstItem.getDescription());
        assertThat(resultFirstItem.getImgPath()).isEqualTo(firstItem.getImgPath());
        assertThat(resultFirstItem.getPrice()).isEqualTo(firstItem.getPrice());

        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(items.size());
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        assertThat(result.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(result.getPageable().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("search one containing")
    void test2() {
        var items = TestDataFactory.createItemsForSave(3);
        var firstItem = items.getFirst();
        var search = "title1";
        var pageable = PageRequest.of(0, 10, Sort.unsorted());
        itemRepository.saveAll(items);

        var result = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                search, search, pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .isNotEmpty()
                .hasSize(1);

        var resultFirstItem = result.getContent().getFirst();
        assertThat(resultFirstItem.getId()).isGreaterThan(0L);
        assertThat(resultFirstItem.getTitle()).isEqualTo(firstItem.getTitle());
        assertThat(resultFirstItem.getDescription()).isEqualTo(firstItem.getDescription());
        assertThat(resultFirstItem.getImgPath()).isEqualTo(firstItem.getImgPath());
        assertThat(resultFirstItem.getPrice()).isEqualTo(firstItem.getPrice());

        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        assertThat(result.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(result.getPageable().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("not containing")
    void test3() {
        var items = TestDataFactory.createItemsForSave(3);
        var search = "title1005001000120312030123";
        var pageable = PageRequest.of(0, 10, Sort.unsorted());
        itemRepository.saveAll(items);

        var result = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                search, search, pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        assertThat(result.getTotalPages()).isEqualTo(0);
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        assertThat(result.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(result.getPageable().getPageSize()).isEqualTo(10);
    }
}
