package ru.practicum.market.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.practicum.market.service.AdminService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public String getAdminPage(Model model) {
        var items = adminService.getAllItems();

        model.addAttribute("items", items);

        return "admin";
    }

    @PostMapping("/items/upload")
    public String uploadItems(@RequestParam MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            adminService.uploadItems(file);
            redirectAttributes.addFlashAttribute("message",
                    "Файл с предметами успешно загружен: \"%s\"".formatted(file.getOriginalFilename()));
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }

        return "redirect:/admin";
    }

    @PostMapping("/items/{id}/image")
    public String uploadImage(@PathVariable long id,
                              @RequestParam MultipartFile image,
                              RedirectAttributes redirectAttributes) {
        try {
            adminService.uploadImage(id, image);
            redirectAttributes.addFlashAttribute("message",
                    "Изображение для товара №%d успешно загружено".formatted(id));
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }

        return "redirect:/admin";
    }
}
