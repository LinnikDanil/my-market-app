package ru.practicum.market.web.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.ItemUploadException;
import ru.practicum.market.service.AdminService;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.web.bind.QueryBinder;
import ru.practicum.market.web.dto.ItemShortResponseDto;
import ru.practicum.market.web.filter.RouteExceptionFilter;
import ru.practicum.market.web.filter.RouteLoggingFilter;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest
@Import({AdminHandlerTest.TestRoutes.class, AdminHandler.class, RouteLoggingFilter.class, RouteExceptionFilter.class})
@DisplayName("AdminHandler")
class AdminHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private QueryBinder binder;

    @TempDir
    Path tempDir;

    @TestConfiguration
    static class TestRoutes {
        @Bean
        RouterFunction<ServerResponse> routes(
                AdminHandler adminHandler,
                RouteLoggingFilter logging,
                RouteExceptionFilter errors
        ) {
            return RouterFunctions.route()
                    .path("/admin", builder -> builder
                            .GET("", adminHandler::getAdminPage)
                            .path("/items", items -> items
                                    .POST("/upload", adminHandler::uploadItems)
                                    .POST("/{id}/image", adminHandler::uploadImage)
                            )
                    )
                    .build()
                    .filter(logging.logging())
                    .filter(errors.errors());
        }
    }

    @Test
    @DisplayName("getAdminPage")
    void test1() {
        var items = List.of(new ItemShortResponseDto(1L, "tit1"), new ItemShortResponseDto(2L, "tit2"));
        when(adminService.getAllItems()).thenReturn(Flux.fromIterable(items));

        webTestClient.get()
                .uri("/admin")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("tit1"));
    }

    @Test
    @DisplayName("uploadItems ok")
    void test2() throws Exception {
        var filePath = tempDir.resolve("items.xlsx");
        Files.writeString(filePath, "content");
        var builder = new org.springframework.http.client.MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(filePath));
        MultiValueMap<String, org.springframework.http.HttpEntity<?>> multipartData = builder.build();

        when(adminService.uploadItems(any(FilePart.class))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/admin/items/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartData))
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().value("Location", location -> {
                    var decoded = URLDecoder.decode(location, StandardCharsets.UTF_8);
                    assertThat(decoded).contains("/admin?message=");
                    assertThat(decoded).contains("Файл с предметами успешно загружен: items.xlsx");
                });

        verify(adminService, times(1)).uploadItems(any(FilePart.class));
    }

    @Test
    @DisplayName("uploadItems error")
    void test3() throws Exception {
        var filePath = tempDir.resolve("items.xlsx");
        Files.writeString(filePath, "content");
        var builder = new org.springframework.http.client.MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(filePath));
        MultiValueMap<String, org.springframework.http.HttpEntity<?>> multipartData = builder.build();

        when(adminService.uploadItems(any(FilePart.class)))
                .thenReturn(Mono.error(new ItemUploadException("upload error")));

        webTestClient.post()
                .uri("/admin/items/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartData))
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().value("Location", location -> {
                    var decoded = URLDecoder.decode(location, StandardCharsets.UTF_8);
                    assertThat(decoded).contains("/admin?error=upload error");
                });
    }

    @Test
    @DisplayName("uploadImage ok")
    void test4() throws Exception {
        var itemId = 5L;
        var filePath = tempDir.resolve("image.png");
        Files.writeString(filePath, "content");
        var builder = new org.springframework.http.client.MultipartBodyBuilder();
        builder.part("image", new FileSystemResource(filePath));
        MultiValueMap<String, org.springframework.http.HttpEntity<?>> multipartData = builder.build();

        when(binder.bindPathVariableId(any())).thenReturn(itemId);
        when(adminService.uploadImage(any(Long.class), any(FilePart.class))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/admin/items/{id}/image", itemId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartData))
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().value("Location", location -> {
                    var decoded = URLDecoder.decode(location, StandardCharsets.UTF_8);
                    assertThat(decoded).contains("/admin?message=Изображение для товара №5 успешно загружено");
                });

        verify(adminService, times(1)).uploadImage(any(Long.class), any(FilePart.class));
    }

    @Test
    @DisplayName("uploadImage error")
    void test5() throws Exception {
        var itemId = 7L;
        var filePath = tempDir.resolve("image.png");
        Files.writeString(filePath, "content");
        var builder = new org.springframework.http.client.MultipartBodyBuilder();
        builder.part("image", new FileSystemResource(filePath));
        MultiValueMap<String, org.springframework.http.HttpEntity<?>> multipartData = builder.build();

        when(binder.bindPathVariableId(any())).thenReturn(itemId);
        when(adminService.uploadImage(any(Long.class), any(FilePart.class)))
                .thenReturn(Mono.error(new RuntimeException("image error")));

        webTestClient.post()
                .uri("/admin/items/{id}/image", itemId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartData))
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().value("Location", location -> {
                    var decoded = URLDecoder.decode(location, StandardCharsets.UTF_8);
                    assertThat(decoded).contains("/admin?error=image error");
                });
    }
}
