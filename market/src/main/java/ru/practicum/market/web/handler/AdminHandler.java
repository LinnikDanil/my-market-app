package ru.practicum.market.web.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.ItemUploadException;
import ru.practicum.market.service.AdminService;
import ru.practicum.market.web.bind.QueryBinder;
import ru.practicum.market.web.view.PageRenderHelper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Обработчик административных HTTP-сценариев: загрузка товаров и изображений.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class AdminHandler {

    private final AdminService adminService;
    private final QueryBinder binder;
    private final PageRenderHelper pageRenderHelper;

    /**
     * Отображает админ-страницу со списком товаров.
     *
     * @param request входящий HTTP-запрос
     * @return серверный ответ с HTML-страницей
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ServerResponse> getAdminPage(ServerRequest request) {
        log.debug("Rendering admin page");
        return adminService.getAllItems()
                .collectList()
                .flatMap(items -> pageRenderHelper.ok(request, "admin", Map.of("items", items)));
    }

    /**
     * Принимает Excel-файл товаров и перенаправляет на страницу admin с сообщением.
     *
     * @param request входящий HTTP-запрос
     * @return редирект на `/admin` с результатом загрузки
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ServerResponse> uploadItems(ServerRequest request) {
        log.info("Handling admin item upload request");
        return request.multipartData()
                .flatMap(parts -> {
                    var part = parts.getFirst("file");
                    if (!(part instanceof FilePart filePart)) {
                        return Mono.error(new ItemUploadException("Файл не найден"));
                    }

                    log.info("Uploading items from file '{}'", filePart.filename());
                    return adminService.uploadItems(filePart)
                            .thenReturn("Файл с предметами успешно загружен: %s".formatted(filePart.filename()));
                })
                .flatMap(msg -> redirectWithParam("message", msg))
                .onErrorResume(ex -> {
                    log.warn("Failed to upload items file", ex);
                    return redirectWithParam("error", ex.getMessage());
                });
    }

    /**
     * Загружает изображение для товара и перенаправляет на страницу admin с сообщением.
     *
     * @param request входящий HTTP-запрос
     * @return редирект на `/admin` с результатом загрузки изображения
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ServerResponse> uploadImage(ServerRequest request) {
        var id = binder.bindPathVariableId(request);
        log.info("Handling image upload for itemId={}", id);

        return request.multipartData()
                .flatMap(parts -> {
                    var part = parts.getFirst("image");
                    if (!(part instanceof FilePart filePart)) {
                        return Mono.error(new IllegalArgumentException("Изображение не найдено"));
                    }

                    log.info("Uploading image '{}' for itemId={}", filePart.filename(), id);
                    return adminService.uploadImage(id, filePart)
                            .thenReturn("Изображение для товара №%d успешно загружено".formatted(id));
                })
                .flatMap(msg -> redirectWithParam("message", msg))
                .onErrorResume(ex -> {
                    log.warn("Failed to upload image for itemId={}", id, ex);
                    return redirectWithParam("error", ex.getMessage());
                });
    }

    /**
     * Выполняет redirect на `/admin` с query-параметром результата.
     *
     * @param key   имя параметра
     * @param value значение параметра
     * @return серверный ответ с редиректом
     */
    private Mono<ServerResponse> redirectWithParam(String key, String value) {
        log.debug("Redirecting to /admin with param {}={}", key, value);
        var uri = UriComponentsBuilder.fromPath("/admin")
                .queryParam(key, value)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        return ServerResponse.seeOther(uri).build();
    }
}
