package ru.practicum.market.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.market.service.AdminService;
import ru.practicum.market.util.TestDataFactory;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AdminController.class)
@DisplayName("AdminController")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @Nested
    @DisplayName("getAdminPage")
    class getAdminPage {

        @Test
        @DisplayName("ok")
        void test1() throws Exception {
            var items = TestDataFactory.createItems(3);
            when(adminService.getAllItems()).thenReturn(items);

            mockMvc.perform(get("/admin"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin"))
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(model().attribute("items", items));
        }
    }

    @Nested
    @DisplayName("uploadItems")
    class uploadItems {

        @Test
        @DisplayName("ok")
        void test1() throws Exception {
            var file = new MockMultipartFile("file", "items.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "content".getBytes());
            doNothing().when(adminService).uploadItems(file);

            mockMvc.perform(multipart("/admin/items/upload").file(file))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin"))
                    .andExpect(flash().attribute("message",
                            "Файл с предметами успешно загружен: \"%s\"".formatted(file.getOriginalFilename())));

            verify(adminService, times(1)).uploadItems(file);
        }

        @Test
        @DisplayName("with error")
        void test2() throws Exception {
            var file = new MockMultipartFile("file", "items.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "content".getBytes());
            doThrow(new RuntimeException("upload error")).when(adminService).uploadItems(file);

            mockMvc.perform(multipart("/admin/items/upload").file(file))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin"))
                    .andExpect(flash().attribute("error", "upload error"));

            verify(adminService, times(1)).uploadItems(file);
        }
    }

    @Nested
    @DisplayName("uploadImage")
    class uploadImage {

        @Test
        @DisplayName("ok")
        void test1() throws Exception {
            var itemId = 10L;
            var image = new MockMultipartFile("image", "image.png", "image/png", "data".getBytes());
            doNothing().when(adminService).uploadImage(itemId, image);

            mockMvc.perform(multipart("/admin/items/{id}/image", itemId).file(image))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin"))
                    .andExpect(flash().attribute("message", "Изображение для товара №%d успешно загружено".formatted(itemId)));

            verify(adminService, times(1)).uploadImage(itemId, image);
        }

        @Test
        @DisplayName("with error")
        void test2() throws Exception {
            var itemId = 5L;
            var image = new MockMultipartFile("image", "image.png", "image/png", "data".getBytes());
            doThrow(new RuntimeException("error"))
                    .when(adminService).uploadImage(anyLong(), any());

            mockMvc.perform(multipart("/admin/items/{id}/image", itemId).file(image))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin"))
                    .andExpect(flash().attribute("error", "error"));

            verify(adminService, times(1)).uploadImage(itemId, image);
        }
    }
}
