package ru.practicum.market.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.dto.enums.SortMethod;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping(path = {"/items", "/"})
    public String getItems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SortMethod sort,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize,
            Model model
    ) {
        var itemsDto = itemService.getItems(search, checkAndGetSortMethod(sort), pageNumber, pageSize);

        model.addAttribute("items", itemsDto.items());
        model.addAttribute("search", itemsDto.search());
        model.addAttribute("sort", itemsDto.sort());
        model.addAttribute("paging", itemsDto.paging());

        return "items";
    }

    @PostMapping("/items")
    public String updateItemsCountInCart(
            @RequestParam long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SortMethod sort,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam CartAction action
    ) {
        itemService.updateItemsCountInCart(id, action);
        return "redirect:/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d"
                .formatted(search, sort, pageNumber, pageSize);
    }

    private SortMethod checkAndGetSortMethod(SortMethod sort) {
        return sort == null ? SortMethod.NO : sort;
    }
}
