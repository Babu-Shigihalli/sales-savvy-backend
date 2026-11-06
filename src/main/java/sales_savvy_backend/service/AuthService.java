package sales_savvy_backend.service;

import sales_savvy_backend.entity.Role;
import sales_savvy_backend.entity.User;
import sales_savvy_backend.repository.RoleRepository;
import sales_savvy_backend.repository.UserRepository;
import sales_savvy_backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public User registerCustomer(String name, String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("CUSTOMER role not found"));

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(customerRole);
        user.setIsActive(true);

        return userRepository.save(user);
    }

    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return jwtUtil.generateToken(user.getEmail(), user.getRole().getName());
    }
    public User registerAdmin(String name, String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(adminRole);
        user.setIsActive(true);

        return userRepository.save(user);
    }
}
