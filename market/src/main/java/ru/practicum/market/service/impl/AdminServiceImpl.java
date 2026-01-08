package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.practicum.market.domain.exception.ItemImageBadRequest;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.domain.exception.ItemUploadException;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.service.AdminService;
import ru.practicum.market.service.converter.ExcelConverter;
import ru.practicum.market.web.dto.ItemShortResponseDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final ItemRepository itemRepository;
    private final ExcelConverter excelConverter;

    @Value("${image.path}")
    private String imagePath;

    /**
     * Загружает Excel-файл с товарами и сохраняет их в базе реактивно.
     *
     * @param file файл Excel с товарами
     * @return сигнал завершения загрузки
     */
    @Override
    @Transactional
    public Mono<Void> uploadItems(FilePart file) {
        excelConverter.checkExcelFormat(file);
        var fileName = file.filename();
        log.debug("Original filename: {}", fileName);
        // Парсим Excel и получаем список товаров.
        return excelConverter.excelToItemList(file)
                // Сохраняем товары реактивно через репозиторий.
                .flatMapMany(itemRepository::saveAll)
                .then()
                .doOnSuccess(v -> log.debug("The Excel file is uploaded: {}", fileName))
                // Приводим неожиданные ошибки к ItemUploadException.
                .onErrorMap(exception -> exception instanceof ItemUploadException
                        ? exception
                        : new ItemUploadException("The Excel file is not upload: %s!".formatted(fileName), exception));
    }

    /**
     * Загружает изображение товара и сохраняет относительный путь в базе.
     *
     * @param id    идентификатор товара
     * @param image файл изображения
     * @return сигнал завершения загрузки
     */
    @Override
    @Transactional
    public Mono<Void> uploadImage(long id, FilePart image) {
        log.debug("Uploading image for itemId={}", id);
        return itemRepository.findById(id)
                // Ищем товар; если нет — возвращаем ошибку.
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id, "Item with id = %d not found!".formatted(id))))
                .flatMap(item -> {
                    // Проверяем наличие изображения.
                    if (image.headers().getContentLength() == 0) {
                        return Mono.error(new ItemImageBadRequest("Image cannot be empty"));
                    }

                    // Логируем метаданные изображения.
                    log.debug("Image content type: {} | size: {} bytes",
                            image.headers().getContentType(), image.headers().getContentLength());

                    return ensureUploadPath()
                            // Готовим безопасный путь сохранения.
                            .map(uploadPath -> buildDestination(uploadPath, image.filename()))
                            // Сохраняем файл на диск.
                            .flatMap(destination -> image.transferTo(destination.filePath())
                                    .thenReturn(destination))
                            .flatMap(destination -> {
                                // Сохраняем относительный путь изображения в товаре.
                                item.setImgPath("/" + imagePath + "/" + destination.safeFileName());
                                return itemRepository.save(item)
                                        .thenReturn(destination);
                            })
                            .doOnSuccess(destination ->
                                    log.debug("Image for item {} saved to {}", id, destination.filePath()))
                            .then();
                });
    }

    /**
     * Возвращает все товары для административной панели в виде коротких DTO.
     *
     * @return поток товаров, отсортированный по названию
     */
    @Transactional(readOnly = true)
    @Override
    public Flux<ItemShortResponseDto> getAllItems() {
        log.debug("Request to fetch all items for admin panel");
        return itemRepository.findAll()
                .map(item -> new ItemShortResponseDto(item.getId(), item.getTitle()))
                .doOnComplete(() -> log.debug("Fetched all items for admin"));
    }

    private Mono<Path> ensureUploadPath() {
        return Mono.fromCallable(() -> {
                    // Создание директории — потенциально блокирующая операция.
                    var uploadPath = Paths.get(imagePath).toAbsolutePath().normalize();
                    Files.createDirectories(uploadPath);
                    return uploadPath;
                })
                // Переносим в boundedElastic и прокидываем ошибку как ItemImageBadRequest.
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(IOException.class, exception ->
                        new ItemImageBadRequest("Failed to create upload directory!", exception));
    }

    private ImageDestination buildDestination(Path uploadPath, String originalFilename) {
        var sanitizedName = StringUtils.hasText(originalFilename)
                // Нормализуем имя файла, отбрасывая пути.
                ? Paths.get(originalFilename).getFileName().toString()
                : "";

        // Извлекаем расширение.
        var extension = "";
        int dotIndex = sanitizedName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < sanitizedName.length() - 1) {
            extension = sanitizedName.substring(dotIndex);
        }

        // Строим безопасный путь.
        var safeFileName = UUID.randomUUID() + extension;
        var filePath = uploadPath.resolve(safeFileName).normalize();

        if (!filePath.startsWith(uploadPath)) {
            throw new ItemImageBadRequest("Invalid image path");
        }

        return new ImageDestination(filePath, safeFileName);
    }

    private record ImageDestination(Path filePath, String safeFileName) {
    }
}
