package com.ecommerce.admin.controller;

import com.ecommerce.library.model.Customer;
import com.ecommerce.library.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/customers")
    public String getAllCustomers(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        } else {
            List<Customer> customerList = customerService.findAllCustomers();
            model.addAttribute("customers", customerList);
            return "customers";
        }
    }

    @PostMapping("/disable-customer")
    public String disableCustomer(@RequestParam("id") Long id, RedirectAttributes attributes) {
        try {
            customerService.disableCustomer(id);
            attributes.addFlashAttribute("successMessage", "Customer has been disabled successfully!");
        } catch (Exception e) {
            attributes.addFlashAttribute("errorMessage", "Failed to disable customer: " + e.getMessage());
        }
        return "redirect:/customers"; // Chuyển hướng về trang danh sách khách hàng của admin
    }

    @PostMapping("/enable-customer")
    public String enableCustomer(@RequestParam("id") Long id, RedirectAttributes attributes) {
        try {
            customerService.enableCustomer(id);
            attributes.addFlashAttribute("successMessage", "Customer has been enabled successfully!");
        } catch (Exception e) {
            attributes.addFlashAttribute("errorMessage", "Failed to enable customer: " + e.getMessage());
        }
        return "redirect:/customers";
    }

}
