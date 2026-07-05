package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Used when seeding data — avoid duplicate categories
    Optional<Category> findByCategoryName(String categoryName);

    boolean existsByCategoryName(String categoryName);
}