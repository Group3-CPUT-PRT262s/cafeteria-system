package com.group3.cafeteria_system.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity 
/* this tells Spring this class maps to a database table. JPA will create the table automatically on startup.
 */
@Table(name = "menu_items")
// this specifies the name of the table in the database. If not provided, it defaults to the class name (menu_item).
public class MenuItem {

    @Id // primary key setter (tells our database that this is a primary key entry (the menuItemId))
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long menuItemId;

    // FK reference to Category table
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    private String description;

    @Column(nullable = false)
    private Double price;

    // Three states: Available, Low Stock, Sold Out
    // Replaces the boolean is_available from the ERD
    // as it carries more information for the staff panel
    @Column(nullable = false)
    private String status = "Available";

    /* Soft delete flag — false hides item from
     student menu without removing the database record
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at") // timestamp
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {createdAt = LocalDateTime.now();}

    // Constructor
    public MenuItem() {}

    public MenuItem(Long categoryId, String itemName, String description, Double price, String status) {
        this.categoryId = categoryId;
        this.itemName = itemName;
        this.description = description;
        this.price = price;
        this.status = status;
        this.isActive = true;
    }

    // Getters

    public Long getMenuItemId() { return menuItemId; }
    public Long getCategoryId() { return categoryId; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public Double getPrice() { return price; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Boolean getIsActive() { return isActive; }
    // ... and Setters

    public void setMenuItemId(Long menuItemId) {this.menuItemId = menuItemId;}
    public void setCategoryId(Long categoryId) {this.categoryId = categoryId;}
    public void setItemName(String itemName) {this.itemName = itemName;}
    public void setDescription(String description) {this.description = description;}
    public void setPrice(Double price) { this.price = price; }
    public void setStatus(String status) { this.status = status; }
    public void setIsActive(Boolean isActive) {this.isActive = isActive;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}


    // Business logic methods
    // Item is available if active AND not sold out
    public boolean isAvailable() {
        return isActive && !status.equals("Sold Out");
    }

    // Soft delete — hides from students, preserves historical order records
    public void softDelete() {this.isActive = false;}

    public void restore() {this.isActive = true;}
}