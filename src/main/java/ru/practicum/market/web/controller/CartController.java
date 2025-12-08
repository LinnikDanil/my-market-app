package ru.practicum.market.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.web.dto.enums.CartAction;

@Controller
@RequestMapping("/cart/items")
@RequiredArgsConstructor
@Log4j2
public class CartController {

    private final ItemService itemService;

    @GetMapping
    public String getCart(Model model) {
        var cart = itemService.getCart();

        model.addAttribute("items", cart.items());
        model.addAttribute("total", cart.total());

        return "cart";
    }

    @PostMapping
    public String updateItemsCountInCart(@RequestParam long id, @RequestParam CartAction action, Model model) {
        log.debug("Update items count in cart with id {} action {}", id, action);

        itemService.updateItemsCountInCart(id, action);

        return getCart(model);
    }
}
