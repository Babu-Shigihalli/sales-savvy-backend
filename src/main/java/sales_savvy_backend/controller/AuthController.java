package sales_savvy_backend.controller;

import sales_savvy_backend.dto.AuthResponse;
import sales_savvy_backend.dto.LoginRequest;
import sales_savvy_backend.dto.RegisterRequest;
import sales_savvy_backend.entity.User;
import sales_savvy_backend.security.JwtUtil;
import sales_savvy_backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public String register(@RequestBody RegisterRequest request) {
        User user = authService.registerCustomer(request.getName(), request.getEmail(), request.getPassword());
        return "User registered successfully with email: " + user.getEmail();
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        String role = jwtUtil.extractRole(token);
        return new AuthResponse(token, request.getEmail(), role);
    }
}