package ru.practicum.market.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.market.domain.exception.ItemImageBadRequest;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.domain.exception.ItemUploadException;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.service.AdminService;
import ru.practicum.market.web.dto.enums.SortMethod;
import ru.practicum.market.web.mapper.ExcelMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class AdminServiceImpl implements AdminService {

    private final ItemRepository itemRepository;

    @Value("${image.path}")
    private String imagePath;

    @Override
    @Transactional
    public void uploadItems(MultipartFile file) {
        ExcelMapper.checkExcelFormat(file);
        log.info("Original filename: {}", file.getOriginalFilename());
        log.info("Content type: {}", file.getContentType());
        try {
            itemRepository.saveAll(ExcelMapper.excelToItemList(file.getInputStream()));
            log.info("The Excel file is uploaded: {}", file.getOriginalFilename());
        } catch (Exception exception) {
            throw new ItemUploadException("The Excel file is not upload: %s!"
                    .formatted(file.getOriginalFilename()), exception);
        }
    }

    @Override
    @Transactional
    public void uploadImage(long id, MultipartFile image) {
        var item = itemRepository.findById(id).orElseThrow(
                () -> new ItemNotFoundException("Item with id = %d not found!".formatted(id))
        );

        if (image.isEmpty()) {
            throw new ItemImageBadRequest("Image cannot be empty");
        }

        // Директория
        var uploadPath = Paths.get(imagePath);

        // Создадим, если не существует
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException exception) {
            throw new ItemImageBadRequest("Failed to create upload directory!", exception);
        }

        // Генерируем уникальное имя изображения
        String fileName = image.getOriginalFilename() + "_" + UUID.randomUUID();

        Path filePath = uploadPath.resolve(fileName);

        // Сохраняем изображение
        try (InputStream is = image.getInputStream()) {
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ItemImageBadRequest("Failed to save image file!", e);
        }

        item.setImgPath("/" + imagePath + "/" + fileName);
        itemRepository.save(item);

    }

    @Transactional(readOnly = true)
    @Override
    public List<Item> getAllItems() {
        return itemRepository.findAll(Sort.by(SortMethod.ALPHA.getColumnName()));
    }
}
