package sales_savvy_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sales_savvy_backend.entity.CartItem;
import sales_savvy_backend.entity.Order;
import sales_savvy_backend.entity.OrderItem;
import sales_savvy_backend.entity.OrderStatus;
import sales_savvy_backend.entity.User;
import sales_savvy_backend.repository.CartItemRepository;
import sales_savvy_backend.repository.OrderRepository;
import sales_savvy_backend.repository.ProductRepository;
import sales_savvy_backend.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public Order placeOrder(String email, String shippingAddress) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cannot place an order with an empty cart");
        }

        // Reserve the stock first. If any item is short, the exception rolls everything back.
        for (CartItem item : cartItems) {
            int updated = productRepository.decreaseStock(item.getProduct().getId(), item.getQuantity());
            if (updated == 0) {
                throw new RuntimeException("Not enough stock for " + item.getProduct().getName());
            }
        }

        BigDecimal total = cartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(total);
        order.setShippingAddress(shippingAddress);

        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(cartItem.getProduct().getPrice());
            return orderItem;
        }).collect(Collectors.toList());

        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        cartItemRepository.deleteByUserId(user.getId());

        return savedOrder;
    }

    public List<Order> getOrdersForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUserId(user.getId());
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    // Customers can only see their own orders. Admins can see any order.
    public Order getOrderForUser(Integer orderId, String email, boolean isAdmin) {
        Order order = getOrderById(orderId);
        if (!isAdmin && !order.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Order not found");
        }
        return order;
    }

    @Transactional
    public Order cancelMyOrder(Integer orderId, String email) {
        Order order = getOrderForUser(orderId, email, false);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Only orders that have not been paid can be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        restoreStock(order);
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.CANCELLED && newStatus != OrderStatus.CANCELLED) {
            throw new RuntimeException("A cancelled order cannot be reopened");
        }
        if (order.getStatus() != OrderStatus.CANCELLED && newStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
        }
    }
}
