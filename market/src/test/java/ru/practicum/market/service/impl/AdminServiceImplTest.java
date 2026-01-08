package ru.practicum.market.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.ItemImageBadRequest;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.domain.exception.ItemUploadException;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.service.converter.ExcelConverter;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.market.web.dto.ItemShortResponseDto;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminServiceImpl")
class AdminServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ExcelConverter excelConverter;

    @InjectMocks
    private AdminServiceImpl adminService;

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("uploadItems")
    class uploadItems {

        @Test
        @DisplayName("ok")
        void test1() {
            var file = org.mockito.Mockito.mock(FilePart.class);
            var items = TestDataFactory.createItems(3);

            when(file.filename()).thenReturn("items.xlsx");
            doNothing().when(excelConverter).checkExcelFormat(file);
            when(excelConverter.excelToItemList(file)).thenReturn(Mono.just(items));
            when(itemRepository.saveAll(items)).thenReturn(Flux.fromIterable(items));

            adminService.uploadItems(file).block();

            verify(excelConverter, times(1)).checkExcelFormat(file);
            verify(excelConverter, times(1)).excelToItemList(file);
            verify(itemRepository, times(1)).saveAll(eq(items));
        }

        @Test
        @DisplayName("exception")
        void test2() {
            var file = org.mockito.Mockito.mock(FilePart.class);

            when(file.filename()).thenReturn("items.xlsx");
            doNothing().when(excelConverter).checkExcelFormat(file);
            when(excelConverter.excelToItemList(file)).thenReturn(Mono.error(new RuntimeException("parse error")));

            assertThatExceptionOfType(ItemUploadException.class)
                    .isThrownBy(() -> adminService.uploadItems(file).block())
                    .withMessageContaining("The Excel file is not upload");

            verify(itemRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("getAllItems")
    class getAllItems {

        @Test
        @DisplayName("ok")
        void test1() {
            var item1 = TestDataFactory.createItem(1L);
            item1.setTitle("b-title");
            var item2 = TestDataFactory.createItem(2L);
            item2.setTitle("A-title");

            when(itemRepository.findAll()).thenReturn(Flux.fromIterable(List.of(item1, item2)));

            var result = adminService.getAllItems().collectList().block();
            assertThat(result)
                    .contains(
                            new ItemShortResponseDto(item1.getId(), item1.getTitle()),
                            new ItemShortResponseDto(item2.getId(), item2.getTitle())
                    );
        }
    }

    @Nested
    @DisplayName("uploadImage")
    class uploadImage {

        @Test
        @DisplayName("ok")
        void test1() {
            var itemId = 1L;
            var item = TestDataFactory.createItem(itemId);
            var image = org.mockito.Mockito.mock(FilePart.class);
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(10);

            ReflectionTestUtils.setField(adminService, "imagePath", tempDir.resolve("images").toString());
            when(itemRepository.findById(itemId)).thenReturn(Mono.just(item));
            when(image.headers()).thenReturn(headers);
            when(image.filename()).thenReturn("image.png");
            when(image.transferTo(any(Path.class))).thenReturn(Mono.empty());
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            adminService.uploadImage(itemId, image).block();

            ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
            verify(itemRepository, times(1)).save(itemCaptor.capture());

            var savedItem = itemCaptor.getValue();
            assertThat(savedItem.getId()).isEqualTo(itemId);
            assertThat(savedItem.getImgPath().replace("\\", "/"))
                    .contains("/images/")
                    .endsWith(".png");
        }

        @Test
        @DisplayName("image empty")
        void test2() {
            var itemId = 2L;
            var image = org.mockito.Mockito.mock(FilePart.class);
            var headers = new HttpHeaders();
            headers.setContentLength(0);

            when(itemRepository.findById(itemId)).thenReturn(Mono.just(TestDataFactory.createItem(itemId)));
            when(image.headers()).thenReturn(headers);

            assertThatExceptionOfType(ItemImageBadRequest.class)
                    .isThrownBy(() -> adminService.uploadImage(itemId, image).block());

            verify(itemRepository, never()).save(any());
        }

        @Test
        @DisplayName("not found")
        void test3() {
            var itemId = 3L;
            var image = org.mockito.Mockito.mock(FilePart.class);

            when(itemRepository.findById(itemId)).thenReturn(Mono.empty());

            assertThatExceptionOfType(ItemNotFoundException.class)
                    .isThrownBy(() -> adminService.uploadImage(itemId, image).block());

            verify(itemRepository, never()).save(any());
        }
    }
}
