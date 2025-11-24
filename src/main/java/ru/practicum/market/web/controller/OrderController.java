package ru.practicum.market.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.market.service.OrderService;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public String getOrders(Model model) {
        var orders = orderService.getOrders();

        model.addAttribute("orders", orders);

        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String getOrder(
            @PathVariable long id,
            @RequestParam(defaultValue = "false") boolean newOrder,
            Model model
    ) {
        var order = orderService.getOrder(id);

        model.addAttribute("order", order);
        model.addAttribute("newOrder", newOrder);

        return "order";
    }

    @PostMapping("/buy")
    public String createOrder() {
        var orderId = orderService.createOrder();

        return "redirect:/orders/%d?newOrder=true".formatted(orderId);
    }
}
