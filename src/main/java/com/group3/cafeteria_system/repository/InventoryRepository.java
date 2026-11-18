package com.group3.cafeteria_system.repository;

import com.group3.cafeteria_system.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // One menu item has at most one inventory record
    Optional<Inventory> findByMenuItemId(Long menuItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.menuItemId = :menuItemId")
    Optional<Inventory> findByMenuItemIdForUpdate(@Param("menuItemId") Long menuItemId);

    List<Inventory> findAllByMenuItemIdIn(Collection<Long> menuItemIds);
}