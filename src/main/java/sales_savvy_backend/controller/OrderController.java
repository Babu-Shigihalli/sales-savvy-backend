package sales_savvy_backend.controller;

import sales_savvy_backend.dto.PlaceOrderRequest;
import sales_savvy_backend.dto.UpdateOrderStatusRequest;
import sales_savvy_backend.entity.Order;
import sales_savvy_backend.entity.OrderStatus;
import sales_savvy_backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order placeOrder(Authentication authentication, @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(authentication.getName(), request.getShippingAddress());
    }

    @GetMapping("/my-orders")
    public List<Order> getMyOrders(Authentication authentication) {
        return orderService.getOrdersForUser(authentication.getName());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Integer id) {
        return orderService.getOrderById(id);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Order updateStatus(@PathVariable Integer id, @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateOrderStatus(id, OrderStatus.valueOf(request.getStatus().toUpperCase()));
    }
}