package com.group3.cafeteria_system.service;

import com.group3.cafeteria_system.model.Category;
import com.group3.cafeteria_system.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category addCategory(String name) {
        if (categoryRepository.existsByCategoryName(name)) {
            throw new RuntimeException("Category already exists: " + name);
        }
        return categoryRepository.save(new Category(name));
    }
}