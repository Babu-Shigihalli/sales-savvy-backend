package sales_savvy_backend.controller;

import sales_savvy_backend.dto.AddToCartRequest;
import sales_savvy_backend.dto.UpdateCartRequest;
import sales_savvy_backend.entity.CartItem;
import sales_savvy_backend.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public List<CartItem> getCart(Authentication authentication) {
        return cartService.getCartForUser(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartItem addToCart(Authentication authentication, @RequestBody AddToCartRequest request) {
        return cartService.addToCart(authentication.getName(), request.getProductId(), request.getQuantity());
    }

    @PutMapping("/{cartItemId}")
    public CartItem updateQuantity(Authentication authentication, @PathVariable Integer cartItemId,
                                   @RequestBody UpdateCartRequest request) {
        return cartService.updateQuantity(authentication.getName(), cartItemId, request.getQuantity());
    }

    @DeleteMapping("/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromCart(Authentication authentication, @PathVariable Integer cartItemId) {
        cartService.removeFromCart(authentication.getName(), cartItemId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(Authentication authentication) {
        cartService.clearCart(authentication.getName());
    }
}