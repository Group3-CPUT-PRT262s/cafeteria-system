package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // One menu item has at most one inventory record
    Optional<Inventory> findByMenuItemId(Long menuItemId);
}