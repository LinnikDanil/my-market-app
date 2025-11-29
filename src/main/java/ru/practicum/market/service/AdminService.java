package ru.practicum.market.service;

import org.springframework.web.multipart.MultipartFile;
import ru.practicum.market.domain.model.Item;

import java.util.List;

public interface AdminService {
    void uploadItems(MultipartFile file);

    void uploadImage(long id, MultipartFile image);

    List<Item> getAllItems();
}
