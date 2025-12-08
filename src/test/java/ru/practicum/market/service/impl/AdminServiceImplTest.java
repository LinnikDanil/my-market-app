package ru.practicum.market.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.market.domain.exception.ItemImageBadRequest;
import ru.practicum.market.domain.exception.ItemUploadException;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.service.converter.ExcelConverter;
import ru.practicum.market.util.TestDataFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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

    @Nested
    @DisplayName("uploadItems")
    class uploadItems {

        @Test
        @DisplayName("ok")
        void test1() {
            var file = new MockMultipartFile(
                    "file",
                    "items.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "content".getBytes()
            );
            var items = TestDataFactory.createItems(3);

            when(excelConverter.excelToItemList(any(InputStream.class))).thenReturn(items);

            adminService.uploadItems(file);

            verify(excelConverter, times(1)).checkExcelFormat(file);
            verify(excelConverter, times(1)).excelToItemList(any(InputStream.class));
            verify(itemRepository, times(1)).saveAll(eq(items));
        }
    }

    @Test
    @DisplayName("exception")
    void test2() {
        var file = new MockMultipartFile(
                "file",
                "items.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "content".getBytes()
        );

        when(excelConverter.excelToItemList(any(InputStream.class)))
                .thenThrow(new RuntimeException("excel error"));

        assertThatExceptionOfType(ItemUploadException.class)
                .isThrownBy(() -> adminService.uploadItems(file))
                .withMessage("The Excel file is not upload: %s!".formatted(file.getOriginalFilename()))
                .withCauseInstanceOf(RuntimeException.class);

        verify(excelConverter, times(1)).checkExcelFormat(file);
        verify(excelConverter, times(1)).excelToItemList(any(InputStream.class));
        verify(itemRepository, times(0)).saveAll(any());

    }

    @Nested
    @DisplayName("uploadImage")
    class uploadImage {

        @Test
        @DisplayName("ok")
        void test1() throws IOException {
            var itemId = 1L;
            var tempDir = Files.createTempDirectory("images");
            var imagePath = tempDir.toString();
            ReflectionTestUtils.setField(adminService, "imagePath", imagePath);
            var item = TestDataFactory.createItem(itemId);
            var file = new MockMultipartFile("image", "image.png", "image/png", "bytes".getBytes());

            doReturn(Optional.of(item)).when(itemRepository).findById(itemId);

            adminService.uploadImage(itemId, file);

            ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
            verify(itemRepository, times(1)).save(itemCaptor.capture());

            var savedItem = itemCaptor.getValue();
            assertThat(savedItem.getId()).isEqualTo(itemId);
            assertThat(savedItem.getImgPath())
                    .startsWith("/" + imagePath + "/")
                    .endsWith(".png");

            var storedFileName = Path.of(savedItem.getImgPath().substring(1));
            assertThat(Files.exists(storedFileName)).isTrue();
        }

        @Test
        @DisplayName("image empty")
        void test2() {
            var itemId = 2L;
            ReflectionTestUtils.setField(adminService, "imagePath", "path");
            var file = new MockMultipartFile("image", "image.png", "image/png", new byte[]{});

            doReturn(Optional.of(TestDataFactory.createItem(itemId))).when(itemRepository).findById(itemId);

            assertThatExceptionOfType(ItemImageBadRequest.class)
                    .isThrownBy(() -> adminService.uploadImage(itemId, file))
                    .withMessage("Image cannot be empty");

            verify(itemRepository, times(0)).save(any());
        }
    }
}
