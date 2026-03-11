package ru.practicum.market.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.web.dto.ItemShortResponseDto;

/**
 * Сервис административных операций с товарами.
 */
public interface AdminService {
    /**
     * Загружает список товаров из Excel-файла.
     *
     * @param file загружаемый Excel-файл
     * @return сигнал завершения операции
     */
    Mono<Void> uploadItems(FilePart file);

    /**
     * Загружает изображение и привязывает его к товару.
     *
     * @param id идентификатор товара
     * @param image файл изображения
     * @return сигнал завершения операции
     */
    Mono<Void> uploadImage(long id, FilePart image);

    /**
     * Возвращает список товаров в коротком формате для admin-экрана.
     *
     * @return поток коротких DTO товаров
     */
    Flux<ItemShortResponseDto> getAllItems();
}
