package ru.practicum.market.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        var sortMethod = sort == null ? SortMethod.NO : sort;
        var itemsDto = itemService.getItems(search, sortMethod, pageNumber, pageSize);

        model.addAttribute("items", itemsDto.items());
        model.addAttribute("search", itemsDto.search());
        model.addAttribute("sort", itemsDto.sort());
        model.addAttribute("paging", itemsDto.paging());

        return "items";
    }

    @GetMapping("/items/{id}")
    public String getItem(@PathVariable long id, Model model) {
        var item = itemService.getItem(id);

        model.addAttribute("item", item);

        return "item";
    }

    @PostMapping("/items")
    public String updateItemsCountInCartForItems(
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

    @PostMapping("/items/{id}")
    public String updateItemsCountInCartForItem(@PathVariable long id, @RequestParam CartAction action, Model model) {
        itemService.updateItemsCountInCart(id, action);

        return getItem(id, model);
    }

}
