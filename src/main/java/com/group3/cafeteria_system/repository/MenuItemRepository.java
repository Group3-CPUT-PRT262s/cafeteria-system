package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    // All active items — what students see
    List<MenuItem> findByIsActiveTrue();

    // Active items filtered by category (now an ID, not a string)
    List<MenuItem> findByIsActiveTrueAndCategoryId(Long categoryId);

    // Active items filtered by status
    List<MenuItem> findByIsActiveTrueAndStatus(String status);
}