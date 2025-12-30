package ru.practicum.market.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.web.dto.ItemShortResponseDto;

public interface AdminService {
    Mono<Void> uploadItems(FilePart file);

    Mono<Void> uploadImage(long id, FilePart image);

    Flux<ItemShortResponseDto> getAllItems();
}
