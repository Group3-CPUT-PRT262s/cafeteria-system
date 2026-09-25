package com.group3.cafeteria_system.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.group3.cafeteria_system.model.Category;
import com.group3.cafeteria_system.repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(Objects.requireNonNull(id, "Category id is required."));
    }

    public Category addCategory(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new NullPointerException("Category name is required, cant be empty or only spaces.");
        }

        String trimmedName = name.trim();
        if (categoryRepository.existsByCategoryName(trimmedName)) {
            throw new IllegalArgumentException("Category already exists: " + trimmedName);
        }
        return categoryRepository.save(new Category(trimmedName));
    }

    public void deleteCategory(Long id) {
        Long categoryId = Objects.requireNonNull(id, "Category id is required.");
        Category category = Objects.requireNonNull(
                categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Category not found.")),
                "Category not found."
        );
        categoryRepository.delete(category);
    }
}
