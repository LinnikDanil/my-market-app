package ru.practicum.market.service;

import org.springframework.web.multipart.MultipartFile;
import ru.practicum.market.web.dto.ItemShortResponseDto;

import java.util.List;

public interface AdminService {
    void uploadItems(MultipartFile file);

    void uploadImage(long id, MultipartFile image);

    List<ItemShortResponseDto> getAllItems();
}
