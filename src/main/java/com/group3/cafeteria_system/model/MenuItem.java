package com.group3.cafeteria_system.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity 
/* this tells Spring this class maps to a database table. JPA will create the table automatically on startup.
 */
@Table(name = "menu_items")
// this specifies the name of the table in the database. If not provided, it defaults to the class name (menu_item).
public class MenuItem {

    @Id // this marks the field as the primary key of the table. (Obvs it’s the unique identifier for each menu item.)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) // this means the name **cannot be null in the database. It’s a required field. (same as @NotNull in validation)
    private String name;

    private String description; 

    @Column(nullable = false) // this means the price **cannot be null in the database.
    private Double price;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false) 
    private String status = "Available";

    @Column(name = "is_active") // this maps the field to a column named "is_active" in the database. It’s used for soft deletion.
    private Boolean isActive = true;

    @Column(name = "created_at") // this maps the field to a column named "created_at" in the database. It stores the timestamp when the menu item was created.
    private LocalDateTime createdAt;


    @PrePersist // this method is called before the entity is persisted (saved) to the database for the first time. It sets the createdAt field to the current date and time.
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors 
    public MenuItem() {}

    /**
     * This constructor is used to create a new menu item with the specified details.
     * The status defaults to "Available" and isActive defaults to true.
     * See class diagram I made for the default values.
     * @param name
     * @param description
     * @param price
     * @param category
     * @param status
     */
    public MenuItem(String name, String description, Double price,
                    String category, String status) { //
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.status = status;
        this.isActive = true;
    }

    // Business logic methods
    public boolean isAvailable() { // checks if item is active and can be ordered. It’s available if it’s active and not sold out.
        return isActive && !status.equals("Sold Out"); //
    }

    public void softDelete() { // if the item is deleted, we set isActive to false instead of removing it from the database. This way we can keep a record of it and restore it if needed.
        this.isActive = false;
    }

    // Getters and Setters 
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}