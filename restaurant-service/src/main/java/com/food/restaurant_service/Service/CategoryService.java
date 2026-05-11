package com.food.restaurant_service.Service;

import com.food.restaurant_service.Entity.Category;
import com.food.restaurant_service.Repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public Category createCategory(String name) {
        // Find existing category to prevent duplicates globally
        Category existing = categoryRepository.findByNameIgnoreCase(name);
        if (existing != null) {
            return existing; // Return existing one instead of creating a duplicate
        }

        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    public List<Category> getAllUniqueCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }
}
