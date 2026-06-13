package com.group3.cafeteria_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryId;

    // FK to menu_items — one inventory record per item
    @Column(name = "menu_item_id", nullable = false, unique = true)
    private Long menuItemId;

    @Column(name = "quantity_in_stock", nullable = false)
    private Integer quantityInStock = 0;

    // When stock falls to or below this level,
    // the item status should be updated to Low Stock
    @Column(name = "reorder_level")
    private Integer reorderLevel = 5;

    // e.g. "portions", "units", "litres"
    @Column(name = "unit_measure")
    private String unitMeasure = "portions";

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {lastUpdated = LocalDateTime.now();}

    // Constructors
    public Inventory() {}

    public Inventory(Long menuItemId, Integer quantityInStock, Integer reorderLevel, String unitMeasure) {
        this.menuItemId = menuItemId;
        this.quantityInStock = quantityInStock;
        this.reorderLevel = reorderLevel;
        this.unitMeasure = unitMeasure;
    }

    /* Business logic methods
    Determines what status the linked MenuItem
    should display based on current stock level
    */
    public String stockLevels() {
        if (quantityInStock <= 0) {
            return "Sold Out";
        } else if (quantityInStock <= reorderLevel) { // how much is left of menu_item
            return "Low Stock";
        } else {
            return "Available";
        }
    }

    public boolean isOutOfStock() {
        return quantityInStock <= 0;
    }

    // Reduce stock when an order is placed
    public void deductStock(int quantity) {
        this.quantityInStock = Math.max(0, this.quantityInStock - quantity);
    }

    // Getters and Setters
    public Long getInventoryId() { return inventoryId; }
    public Long getMenuItemId() { return menuItemId; }
    public Integer getQuantityInStock() {return quantityInStock;}
    public Integer getReorderLevel() { return reorderLevel; }
    public String getUnitMeasure() { return unitMeasure; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }


    public void setInventoryId(Long inventoryId) {this.inventoryId = inventoryId;}
    public void setMenuItemId(Long menuItemId) {this.menuItemId = menuItemId;}
    public void setQuantityInStock(Integer qty) {this.quantityInStock = qty;}
    public void setReorderLevel(Integer reorderLevel) {this.reorderLevel = reorderLevel;}
    public void setUnitMeasure(String unitMeasure) {this.unitMeasure = unitMeasure;}
    public void setLastUpdated(LocalDateTime lastUpdated) {this.lastUpdated = lastUpdated;}
}