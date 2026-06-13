package com.group3.cafeteria_system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    // Category name e.g. Hot Meal, Snack, Drinks
    // Must be unique — no duplicate category names
    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;

    // Constructors
    public Category() {}

    public Category(String categoryName) {
        this.categoryName = categoryName;
    }

    // Getters and Setters
    public Long getCategoryId() { return categoryId; }

    public String getCategoryName() { return categoryName; }

    public void setCategoryId(Long categoryId) {this.categoryId = categoryId;}

    public void setCategoryName(String categoryName) {this.categoryName = categoryName;}
}