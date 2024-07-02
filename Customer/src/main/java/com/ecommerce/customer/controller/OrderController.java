package com.ecommerce.customer.controller;

import com.ecommerce.library.dto.CustomerDto;
import com.ecommerce.library.model.*;
import com.ecommerce.library.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final CustomerService customerService;
    private final OrderService orderService;

    private final ShoppingCartService cartService;

    private final CountryService countryService;

    private final CityService cityService;

    private final VnpayService vnpayService; // Inject VnpayService dependency

    private final MomoService momoService;

    @GetMapping("/check-out")
    public String checkOut(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        } else {
            CustomerDto customer = customerService.getCustomer(principal.getName());
            if (customer.getAddress() == null || customer.getCity() == null || customer.getPhoneNumber() == null) {
                model.addAttribute("information", "You need update your information before check out");
                List<Country> countryList = countryService.findAll();
                List<City> cities = cityService.findAll();
                model.addAttribute("customer", customer);
                model.addAttribute("cities", cities);
                model.addAttribute("countries", countryList);
                model.addAttribute("title", "Profile");
                model.addAttribute("page", "Profile");
                return "customer-information";
            } else {
                ShoppingCart cart = customerService.findByUsername(principal.getName()).getCart();
                model.addAttribute("customer", customer);
                model.addAttribute("title", "Check-Out");
                model.addAttribute("page", "Check-Out");
                model.addAttribute("shoppingCart", cart);
                model.addAttribute("grandTotal", cart.getTotalItems());
                return "checkout";
            }
        }
    }

    @GetMapping("/orders")
    public String getOrders(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        } else {
            Customer customer = customerService.findByUsername(principal.getName());
            List<Order> orderList = customer.getOrders();
            model.addAttribute("orders", orderList);
            model.addAttribute("title", "Order");
            model.addAttribute("page", "Order");
            return "order";
        }
    }

    @RequestMapping(value = "/cancel-order", method = {RequestMethod.PUT, RequestMethod.GET})
    public String cancelOrder(@RequestParam("id") Long id, RedirectAttributes attributes) {
        orderService.cancelOrder(id);
        return "order";
    }


    @RequestMapping(value = "/add-order", method = {RequestMethod.POST})
    public String createOrder(Principal principal,
                              Model model,
                              HttpSession session,
                              @RequestParam("paymentMethod") String paymentMethod,
                              HttpServletRequest request) throws UnsupportedEncodingException {
        if (principal == null) {
            return "redirect:/login";
        } else {
            Customer customer = customerService.findByUsername(principal.getName());
            ShoppingCart cart = customer.getCart();
            Order order;
            if("cash".equals(paymentMethod)) {
                // Handle cash payment flow
                order = orderService.save(cart); // Save order without payment processing for cash on delivery
                session.removeAttribute("totalItems");
                model.addAttribute("order", order);
                model.addAttribute("title", "Order Detail");
                model.addAttribute("page", "Order Detail");
                model.addAttribute("success", "Order placed successfully (Cash on delivery)");
                return "order-detail";
            }else if("vnpay".equals(paymentMethod)){
                // Handle VNPAY payment flow
                order = orderService.save(cart); // Save order without payment processing for VNPAY
                session.removeAttribute("totalItems");
                String returnUrl = "http://localhost:8020/shop/vnpayreturn"; // URL để VNPAY redirect về sau khi thanh toán
                String encodedUrl = URLEncoder.encode(returnUrl, StandardCharsets.UTF_8);
                String paymentUrl = vnpayService.generatePaymentUrl(order, encodedUrl,request);

                // Redirect user to VNPAY payment gateway
                return "redirect:" + paymentUrl;
            }else{
                // Handle invalid payment method case
                model.addAttribute("error", "Invalid payment method selected");
                return "checkout"; // Redirect back to checkout with an error messag
            }
        }
    }

    // Xử lý response từ VNPAY sau khi thanh toán thành công
    @GetMapping("/vnpayreturn")
    public String handleVnpayReturn(@RequestParam("vnp_ResponseCode") String vnpResponseCode,
                                    @RequestParam("vnp_TransactionNo") String vnpTransactionNo,
                                    RedirectAttributes redirectAttributes) {
        if ("00".equals(vnpResponseCode)) { // Kiểm tra mã phản hồi từ VNPAY (tuỳ chỉnh theo cấu hình VNPAY)
            // Lấy thông tin đơn hàng từ session hoặc cơ sở dữ liệu
            Order order = orderService.findById(Long.parseLong(vnpTransactionNo));
            if (order != null) {
                // Cập nhật trạng thái giao dịch hoặc thực hiện hành động cần thiết cho thanh toán thành công
                // Ví dụ: cập nhật trạng thái đơn hàng là đã thanh toán thành công
                orderService.update(order);

                // Redirect về trang đơn hàng hoặc trang cần thiết
                redirectAttributes.addFlashAttribute("order", order);
                return "redirect:/orders"; // Redirect về trang đơn hàng
            }
        }
        // Xử lý thanh toán thất bại hoặc trường hợp không hợp lệ khác
        return "redirect:/checkout"; // Redirect về trang checkout
    }

}
