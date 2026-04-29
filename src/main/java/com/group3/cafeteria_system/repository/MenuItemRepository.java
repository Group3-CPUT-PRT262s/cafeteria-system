package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    // Find all active items (is_active = true)
    // Spring generates the SQL from the method name automatically
    List<MenuItem> findByIsActiveTrue();

    // Find active items by category
    List<MenuItem> findByIsActiveTrueAndCategory(String category);

    // Find active items by status
    List<MenuItem> findByIsActiveTrueAndStatus(String status);
}