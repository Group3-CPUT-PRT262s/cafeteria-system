package com.group3.cafeteria_system.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.group3.cafeteria_system.model.Category;
import com.group3.cafeteria_system.repository.CategoryRepository;
import com.group3.cafeteria_system.repository.MenuItemRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           MenuItemRepository menuItemRepository) {
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(Objects.requireNonNull(id, "Category id is required."));
    }

    public Category addCategory(String name) {
        String trimmedName = normalizeName(name);
        if (categoryRepository.existsByCategoryNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("Category already exists: " + trimmedName);
        }
        return categoryRepository.save(new Category(trimmedName));
    }

    public Category updateCategory(Long id, String name) {
        Category category = getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
        String normalizedName = normalizeName(name);
        if (categoryRepository.existsByCategoryNameIgnoreCaseAndCategoryIdNot(normalizedName, id)) {
            throw new IllegalArgumentException("Category already exists: " + normalizedName);
        }
        category.setCategoryName(normalizedName);
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        Long categoryId = Objects.requireNonNull(id, "Category id is required.");
        Category category = getCategoryById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
        long itemCount = menuItemRepository.countByCategoryId(categoryId);
        if (itemCount > 0) {
            throw new IllegalStateException(
                    "This category is used by " + itemCount
                            + " menu item(s). Reassign those items before deleting the category.");
        }
        categoryRepository.delete(category);
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name is required.");
        }
        return name.trim();
    }
}
