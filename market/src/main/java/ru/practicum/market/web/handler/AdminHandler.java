package ru.practicum.market.web.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.ItemUploadException;
import ru.practicum.market.service.AdminService;
import ru.practicum.market.web.bind.QueryBinder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class AdminHandler {

    private final AdminService adminService;
    private final QueryBinder binder;

    @GetMapping
    public Mono<ServerResponse> getAdminPage(ServerRequest request) {
        return adminService.getAllItems()
                .collectList()
                .flatMap(items -> ServerResponse.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .render("admin", Map.of("items", items)));
    }

    public Mono<ServerResponse> uploadItems(ServerRequest request) {
        return request.multipartData()
                .flatMap(parts -> {
                    var part = parts.getFirst("file");
                    if (!(part instanceof FilePart filePart)) {
                        return Mono.error(new ItemUploadException("Файл не найден"));
                    }

                    return adminService.uploadItems(filePart)
                            .thenReturn("Файл с предметами успешно загружен: %s".formatted(filePart.filename()));
                })
                .flatMap(msg -> redirectWithParam("message", msg))
                .onErrorResume(ex -> redirectWithParam("error", ex.getMessage()));
    }

    public Mono<ServerResponse> uploadImage(ServerRequest request) {
        var id = binder.bindPathVariableId(request);

        return request.multipartData()
                .flatMap(parts -> {
                    var part = parts.getFirst("image");
                    if (!(part instanceof FilePart filePart)) {
                        return Mono.error(new IllegalArgumentException("Изображение не найдено"));
                    }

                    return adminService.uploadImage(id, filePart)
                            .thenReturn("Изображение для товара №%d успешно загружено".formatted(id));
                })
                .flatMap(msg -> redirectWithParam("message", msg))
                .onErrorResume(ex -> redirectWithParam("error", ex.getMessage()));
    }

    private Mono<ServerResponse> redirectWithParam(String key, String value) {
        var uri = UriComponentsBuilder.fromPath("/admin")
                .queryParam(key, value)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        return ServerResponse.seeOther(uri).build();
    }
}
