package sales_savvy_backend.service;

import sales_savvy_backend.entity.Category;
import sales_savvy_backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    public Category updateCategory(Integer id, Category updatedCategory) {
        Category existing = getCategoryById(id);
        existing.setName(updatedCategory.getName());
        existing.setDescription(updatedCategory.getDescription());
        return categoryRepository.save(existing);
    }

    public void deleteCategory(Integer id) {
        categoryRepository.deleteById(id);
    }
}