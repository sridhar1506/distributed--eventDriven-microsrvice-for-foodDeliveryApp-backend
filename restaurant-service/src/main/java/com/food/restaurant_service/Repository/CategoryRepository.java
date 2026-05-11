package com.food.restaurant_service.Repository;

import com.food.restaurant_service.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Category findCategoryById(Long id);

    Category findByNameIgnoreCase(String name);

}
