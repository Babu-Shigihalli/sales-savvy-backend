package sales_savvy_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sales_savvy_backend.entity.CartItem;
import sales_savvy_backend.entity.Order;
import sales_savvy_backend.entity.OrderItem;
import sales_savvy_backend.entity.OrderStatus;
import sales_savvy_backend.entity.Product;
import sales_savvy_backend.entity.User;
import sales_savvy_backend.repository.CartItemRepository;
import sales_savvy_backend.repository.OrderRepository;
import sales_savvy_backend.repository.ProductRepository;
import sales_savvy_backend.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setEmail("customer@example.com");

        product = new Product();
        product.setId(10);
        product.setName("Wireless Mouse");
        product.setPrice(new BigDecimal("100.00"));
        product.setStockQuantity(5);
    }

    private CartItem cartItem(int quantity) {
        CartItem item = new CartItem();
        item.setUser(user);
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }

    private Order orderWith(OrderStatus status, int quantity) {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);

        Order order = new Order();
        order.setId(7);
        order.setUser(user);
        order.setStatus(status);
        order.setItems(List.of(item));
        return order;
    }

    @Test
    void placeOrder_reducesStock_calculatesTotal_andClearsCart() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserId(1)).thenReturn(List.of(cartItem(2)));
        when(productRepository.decreaseStock(10, 2)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order saved = orderService.placeOrder("customer@example.com", "12 Test Street");

        assertEquals(OrderStatus.PENDING, saved.getStatus());
        assertEquals(0, new BigDecimal("200.00").compareTo(saved.getTotalAmount()));
        assertEquals(1, saved.getItems().size());
        verify(productRepository).decreaseStock(10, 2);
        verify(cartItemRepository).deleteByUserId(1);
    }

    @Test
    void placeOrder_withNotEnoughStock_failsAndSavesNothing() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserId(1)).thenReturn(List.of(cartItem(9)));
        when(productRepository.decreaseStock(10, 9)).thenReturn(0);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> orderService.placeOrder("customer@example.com", "12 Test Street"));

        assertTrue(error.getMessage().contains("Not enough stock"));
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByUserId(any());
    }

    @Test
    void placeOrder_withEmptyCart_fails() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserId(1)).thenReturn(List.of());

        assertThrows(RuntimeException.class,
                () -> orderService.placeOrder("customer@example.com", "12 Test Street"));

        verifyNoInteractions(productRepository);
    }

    @Test
    void cancelMyOrder_pendingOrder_restoresStock() {
        when(orderRepository.findById(7)).thenReturn(Optional.of(orderWith(OrderStatus.PENDING, 2)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order cancelled = orderService.cancelMyOrder(7, "customer@example.com");

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        verify(productRepository).increaseStock(10, 2);
    }

    @Test
    void cancelMyOrder_ofAnotherUser_isHidden() {
        when(orderRepository.findById(7)).thenReturn(Optional.of(orderWith(OrderStatus.PENDING, 2)));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> orderService.cancelMyOrder(7, "someone.else@example.com"));

        assertEquals("Order not found", error.getMessage());
        verifyNoInteractions(productRepository);
    }

    @Test
    void cancelMyOrder_paidOrder_isRejected() {
        when(orderRepository.findById(7)).thenReturn(Optional.of(orderWith(OrderStatus.APPROVED, 2)));

        assertThrows(RuntimeException.class,
                () -> orderService.cancelMyOrder(7, "customer@example.com"));

        verify(productRepository, never()).increaseStock(any(), any());
    }

    @Test
    void updateStatus_cancelledOrderCannotBeReopened() {
        when(orderRepository.findById(7)).thenReturn(Optional.of(orderWith(OrderStatus.CANCELLED, 2)));

        assertThrows(RuntimeException.class,
                () -> orderService.updateOrderStatus(7, OrderStatus.PENDING));
    }

    @Test
    void updateStatus_toCancelled_restoresStock() {
        when(orderRepository.findById(7)).thenReturn(Optional.of(orderWith(OrderStatus.APPROVED, 2)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.updateOrderStatus(7, OrderStatus.CANCELLED);

        verify(productRepository).increaseStock(10, 2);
    }
}
