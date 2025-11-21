package ru.practicum.market.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.market.web.dto.ItemsResponseDto;
import ru.practicum.market.web.dto.enums.SortMethod;
import ru.practicum.market.service.ItemService;

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

        if (sort == null) {
            sort = SortMethod.NO;
        }

        ItemsResponseDto itemsDto = itemService.getItems(search, sort, pageNumber, pageSize);

        model.addAttribute("items", itemsDto.items());
        model.addAttribute("search", itemsDto.search());
        model.addAttribute("sort", itemsDto.sort());
        model.addAttribute("paging", itemsDto.paging());

        return "items";
    }
}
