package com.ecommerce.admin.controller;

import com.ecommerce.library.model.Order;
import com.ecommerce.library.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Date;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public String getAll(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        } else {
            List<Order> orderList = orderService.findALlOrders();
            model.addAttribute("orders", orderList);
            return "orders";
        }
    }

    @RequestMapping(value = "/accept-order", method = {RequestMethod.PUT, RequestMethod.GET})
    public String acceptOrder(Long id, RedirectAttributes attributes, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        } else {
            orderService.acceptOrder(id);
            attributes.addFlashAttribute("success", "Order Accepted");
            return "redirect:/orders";
        }
    }

    @RequestMapping(value = "/cancel-order", method = {RequestMethod.PUT, RequestMethod.GET})
    public String cancelOrder(Long id, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        } else {
            orderService.cancelOrder(id);
            return "redirect:/orders";
        }
    }

    @RequestMapping(value = "/update-delivery-date", method = {RequestMethod.POST})
    public String updateDeliveryDate(@RequestParam("id") Long id, @RequestParam("deliveryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date deliveryDate, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        } else {
            orderService.updateDeliveryDate(id, deliveryDate);
            return "redirect:/orders";
        }
    }

    @GetMapping("/index")
    public String getDashboard(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        } else {
            long orderCount = orderService.countAllOrders();
            double totalIncome = orderService.getTotalIncome();
            model.addAttribute("orderCount", orderCount);
            model.addAttribute("totalIncome", totalIncome);
            return "index";
        }
    }
}
