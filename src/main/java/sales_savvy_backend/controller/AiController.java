package sales_savvy_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sales_savvy_backend.service.AiService;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/description")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> generateDescription(@RequestBody Map<String, String> request) {
        String name = request.getOrDefault("name", "").trim();
        String category = request.getOrDefault("category", "").trim();

        if (name.isEmpty()) {
            throw new RuntimeException("Enter a product name first");
        }
        if (name.length() > 100 || category.length() > 100) {
            throw new RuntimeException("Name and category must be under 100 characters");
        }

        return Map.of("description", aiService.generateDescription(name, category));
    }
}
