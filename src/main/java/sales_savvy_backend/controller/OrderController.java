package sales_savvy_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sales_savvy_backend.dto.PlaceOrderRequest;
import sales_savvy_backend.dto.UpdateOrderStatusRequest;
import sales_savvy_backend.entity.Order;
import sales_savvy_backend.entity.OrderStatus;
import sales_savvy_backend.service.OrderService;

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
    public Order getOrderById(@PathVariable Integer id, Authentication authentication) {
        return orderService.getOrderForUser(id, authentication.getName(), isAdmin(authentication));
    }

    @PutMapping("/{id}/cancel")
    public Order cancelOrder(@PathVariable Integer id, Authentication authentication) {
        return orderService.cancelMyOrder(id, authentication.getName());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Order updateStatus(@PathVariable Integer id, @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateOrderStatus(id, OrderStatus.valueOf(request.getStatus().toUpperCase()));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
