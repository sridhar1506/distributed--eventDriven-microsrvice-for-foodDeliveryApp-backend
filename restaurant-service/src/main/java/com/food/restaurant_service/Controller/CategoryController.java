package com.food.restaurant_service.Controller;

import com.food.restaurant_service.Entity.Category;
import com.food.restaurant_service.Service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.food.restaurant_service.dto.CreateCategoryRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/restaurants/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllUniqueCategories());
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CreateCategoryRequest req) {
        return new ResponseEntity<>(categoryService.createCategory(req.getName()), HttpStatus.CREATED);
    }

}
