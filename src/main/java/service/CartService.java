package sales_savvy_backend.service;

import sales_savvy_backend.entity.CartItem;
import sales_savvy_backend.entity.Product;
import sales_savvy_backend.entity.User;
import sales_savvy_backend.repository.CartItemRepository;
import sales_savvy_backend.repository.ProductRepository;
import sales_savvy_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<CartItem> getCartForUser(String email) {
        User user = getUserByEmail(email);
        return cartItemRepository.findByUserId(user.getId());
    }

    public CartItem addToCart(String email, Integer productId, Integer quantity) {
        User user = getUserByEmail(email);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        return cartItemRepository.findByUserIdAndProductId(user.getId(), productId)
                .map(existingItem -> {
                    existingItem.setQuantity(existingItem.getQuantity() + quantity);
                    return cartItemRepository.save(existingItem);
                })
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setUser(user);
                    newItem.setProduct(product);
                    newItem.setQuantity(quantity);
                    return cartItemRepository.save(newItem);
                });
    }

    public CartItem updateQuantity(String email, Integer cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        validateOwnership(item, email);

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return null;
        }

        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    public void removeFromCart(String email, Integer cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        validateOwnership(item, email);
        cartItemRepository.delete(item);
    }

    public void clearCart(String email) {
        User user = getUserByEmail(email);
        cartItemRepository.deleteByUserId(user.getId());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validateOwnership(CartItem item, String email) {
        if (!item.getUser().getEmail().equals(email)) {
            throw new RuntimeException("You do not have permission to modify this cart item");
        }
    }
}